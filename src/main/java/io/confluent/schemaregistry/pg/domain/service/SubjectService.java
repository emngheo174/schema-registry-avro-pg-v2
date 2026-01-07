package io.confluent.schemaregistry.pg.domain.service;

import io.confluent.schemaregistry.pg.api.exception.IncompatibleSchemaException;
import io.confluent.schemaregistry.pg.api.exception.SubjectNotFoundException;
import io.confluent.schemaregistry.pg.api.exception.VersionNotFoundException;
import io.confluent.schemaregistry.pg.domain.model.SchemaEntity;
import io.confluent.schemaregistry.pg.domain.model.SubjectVersion;
import io.confluent.schemaregistry.pg.domain.value.CompatibilityLevel;
import io.confluent.schemaregistry.pg.domain.value.Md5Hash;
import io.confluent.schemaregistry.pg.domain.value.Mode;
import io.confluent.schemaregistry.pg.domain.value.SchemaId;
import io.confluent.schemaregistry.pg.domain.value.SubjectName;
import io.confluent.schemaregistry.pg.domain.value.Version;
import io.confluent.schemaregistry.pg.infrastructure.persistence.ConfigRepository;
import io.confluent.schemaregistry.pg.infrastructure.persistence.SubjectVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for subject-related operations.
 */
@Service
public class SubjectService {

    private final SubjectVersionRepository subjectVersionRepository;
    private final SchemaService schemaService;
    private final ConfigRepository configRepository;
    private final CompatibilityCheckService compatibilityCheckService;

    public SubjectService(SubjectVersionRepository subjectVersionRepository,
                          SchemaService schemaService,
                          ConfigRepository configRepository,
                          CompatibilityCheckService compatibilityCheckService) {
        this.subjectVersionRepository = subjectVersionRepository;
        this.schemaService = schemaService;
        this.configRepository = configRepository;
        this.compatibilityCheckService = compatibilityCheckService;
    }

    public List<String> listSubjects(String subjectPrefix, boolean deleted, boolean deletedOnly) {
        if (deletedOnly) {
            return subjectVersionRepository.findDeletedSubjects();
        }

        if (subjectPrefix != null && !subjectPrefix.isBlank()) {
            return subjectVersionRepository.findSubjectsWithPrefix(subjectPrefix, deleted);
        }

        return subjectVersionRepository.findAllSubjects(deleted);
    }

    public SubjectVersion lookupSchema(SubjectName subjectName, Md5Hash schemaHash) {
        Optional<SubjectVersion> sv = subjectVersionRepository.findBySubjectAndHash(subjectName, schemaHash);
        if (sv.isEmpty()) {
            throw new SubjectNotFoundException(subjectName.getValue());
        }
        return sv.get();
    }

    @Transactional
    public List<Integer> deleteSubject(SubjectName subjectName, boolean permanent) {
        if (!subjectVersionRepository.existsBySubject(subjectName)) {
            throw new SubjectNotFoundException(subjectName.getValue());
        }

        List<Integer> versions = subjectVersionRepository.findVersionsBySubject(subjectName, true);

        if (permanent) {
            subjectVersionRepository.hardDeleteAllVersions(subjectName);
            configRepository.deleteSubjectConfig(subjectName);
        } else {
            subjectVersionRepository.softDeleteAllVersions(subjectName);
        }

        return versions;
    }

    public List<Integer> listVersions(SubjectName subjectName, boolean deleted) {
        List<Integer> versions = subjectVersionRepository.findVersionsBySubject(subjectName, deleted);
        if (versions.isEmpty() && !deleted) {
            throw new SubjectNotFoundException(subjectName.getValue());
        }
        return versions;
    }

    @Transactional
    public SubjectVersion registerVersion(SubjectName subjectName, SchemaEntity schema, Integer explicitId) {
        // Check mode
        Mode mode = configRepository.getEffectiveMode(subjectName);
        if (!mode.isWriteAllowed()) {
            throw new IllegalStateException("Write operations not allowed in " + mode + " mode");
        }

        // Register or get existing schema
        SchemaEntity savedSchema = schemaService.registerOrGetExisting(schema, explicitId);

        // Check if this schema is already registered under this subject
        Optional<SubjectVersion> existingSv = subjectVersionRepository.findBySubjectAndHash(
                subjectName, savedSchema.getMd5Hash());
        if (existingSv.isPresent()) {
            return existingSv.get();
        }

        // Get compatibility level and check compatibility (unless in IMPORT mode)
        if (!mode.isImportMode()) {
            CompatibilityLevel compatibilityLevel = configRepository.getEffectiveCompatibility(subjectName);
            checkCompatibility(subjectName, schema.getSchemaText(), compatibilityLevel);
        }

        // Determine next version number
        int nextVersion = subjectVersionRepository.getNextVersion(subjectName);

        // Create subject-version mapping
        SubjectVersion subjectVersion = SubjectVersion.builder()
                .subject(subjectName)
                .version(Version.of(nextVersion))
                .schemaId(savedSchema.getId())
                .deleted(false)
                .createdAt(Instant.now())
                .build();

        return subjectVersionRepository.save(subjectVersion);
    }

    private void checkCompatibility(SubjectName subjectName, String newSchemaText, CompatibilityLevel compatibilityLevel) {
        // Get existing schemas (newest first)
        List<Integer> versions = subjectVersionRepository.findVersionsBySubject(subjectName, false);
        if (versions.isEmpty()) {
            return; // No existing versions, so compatible
        }

        List<String> existingSchemas = new ArrayList<>();
        for (int i = versions.size() - 1; i >= 0; i--) {
            Version version = Version.of(versions.get(i));
            Optional<SubjectVersion> sv = subjectVersionRepository.findBySubjectAndVersion(subjectName, version, false);
            if (sv.isPresent()) {
                SchemaEntity schema = schemaService.getById(sv.get().getSchemaId());
                existingSchemas.add(schema.getSchemaText());
            }
        }

        List<String> errors = compatibilityCheckService.checkCompatibility(
                newSchemaText, existingSchemas, compatibilityLevel);

        if (!errors.isEmpty()) {
            throw new IncompatibleSchemaException(
                    subjectName.getValue(),
                    compatibilityLevel.name(),
                    String.join("; ", errors)
            );
        }
    }

    public SubjectVersion getVersion(SubjectName subjectName, Version version, boolean deleted) {
        Optional<SubjectVersion> sv = subjectVersionRepository.findBySubjectAndVersion(subjectName, version, deleted);
        if (sv.isEmpty()) {
            if (version.isLatest()) {
                throw new SubjectNotFoundException(subjectName.getValue());
            }
            throw new VersionNotFoundException(subjectName.getValue(), version.getValue());
        }
        return sv.get();
    }

    @Transactional
    public int deleteVersion(SubjectName subjectName, Version version, boolean permanent) {
        if (!subjectVersionRepository.existsBySubjectAndVersion(subjectName, version)) {
            throw new VersionNotFoundException(subjectName.getValue(), version.getValue());
        }

        if (permanent) {
            subjectVersionRepository.hardDelete(subjectName, version);
        } else {
            subjectVersionRepository.softDelete(subjectName, version);
        }

        return version.getValue();
    }
}
