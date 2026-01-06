package com.example.schemaregistry.domain.service;

import com.example.schemaregistry.domain.model.SchemaEntity;
import com.example.schemaregistry.domain.model.SubjectVersion;
import com.example.schemaregistry.domain.value.CompatibilityLevel;
import com.example.schemaregistry.domain.value.Md5Hash;
import com.example.schemaregistry.domain.value.SchemaId;
import com.example.schemaregistry.domain.value.SchemaType;
import com.example.schemaregistry.domain.value.SubjectName;
import com.example.schemaregistry.domain.value.Version;
import com.example.schemaregistry.infrastructure.persistence.SchemaRepository;
import com.example.schemaregistry.infrastructure.persistence.SubjectConfigRepository;
import com.example.schemaregistry.infrastructure.persistence.SubjectVersionRepository;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.SchemaParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SchemaService {

    private final SchemaRepository schemaRepository;
    private final SubjectVersionRepository subjectVersionRepository;
    private final SubjectConfigRepository subjectConfigRepository;

    @Value("${schema.registry.compatibility.level:BACKWARD}")
    private String compatibilityLevel;

    public SchemaService(SchemaRepository schemaRepository,
                        SubjectVersionRepository subjectVersionRepository,
                        SubjectConfigRepository subjectConfigRepository) {
        this.schemaRepository = schemaRepository;
        this.subjectVersionRepository = subjectVersionRepository;
        this.subjectConfigRepository = subjectConfigRepository;
    }

    private CompatibilityLevel getCompatibilityLevel(String subject) {
        return subjectConfigRepository.findBySubject(SubjectName.of(subject))
                .map(SubjectConfig::getCompatibilityLevel)
                .orElse(CompatibilityLevel.from(compatibilityLevel));
    }

    @Transactional
    public SchemaId registerSchema(String subject, String schemaText) {
        // Validate Avro schema
        try {
            new org.apache.avro.Schema.Parser().parse(schemaText);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Avro schema: " + e.getMessage());
        }

        SubjectName subjectName = SubjectName.of(subject);

        // Check compatibility with latest version
        checkCompatibilityForRegistration(subjectName, schemaText);

        // Get next version number
        int nextVersion = getNextVersion(subjectName);

        // Create schema entity with deduplication
        SchemaEntity schemaEntity = SchemaEntity.builder()
                .schemaText(schemaText)
                .schemaType(SchemaType.AVRO)
                .md5Hash(Md5Hash.compute(schemaText))
                .build();

        // Save schema (deduplication happens here)
        SchemaId schemaId = saveSchemaWithDeduplication(schemaEntity);

        // Create subject-version mapping
        SubjectVersion subjectVersion = SubjectVersion.builder()
                .subject(subjectName)
                .version(Version.of(nextVersion))
                .schemaId(schemaId)
                .build();

        subjectVersionRepository.save(subjectVersion);

        return schemaId;
    }

    private void checkCompatibilityForRegistration(SubjectName subject, String schemaText) {
        Optional<SubjectVersion> latestOpt = subjectVersionRepository.findLatestBySubject(subject);
        if (latestOpt.isEmpty()) {
            return; // First schema, no compatibility check needed
        }

        SubjectVersion latest = latestOpt.get();
        SchemaEntity existingSchema = schemaRepository.findById(latest.getSchemaId())
                .orElseThrow(() -> new IllegalStateException("Schema not found: " + latest.getSchemaId()));

        try {
            org.apache.avro.Schema newParsed = new org.apache.avro.Schema.Parser().parse(schemaText);
            org.apache.avro.Schema existingParsed = new org.apache.avro.Schema.Parser().parse(existingSchema.getSchemaText());

            CompatibilityLevel level = getCompatibilityLevel(subject.getValue());
            if (!checkCompatibility(newParsed, existingParsed, level)) {
                throw new IllegalArgumentException("Schema is not compatible. Level: " + level);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Schema validation error: " + e.getMessage());
        }
    }

    private int getNextVersion(SubjectName subject) {
        return subjectVersionRepository.countBySubjectAndDeletedFalse(subject) + 1;
    }

    private SchemaId saveSchemaWithDeduplication(SchemaEntity schema) {
        // Check if schema already exists by MD5 hash
        Optional<SchemaEntity> existing = schemaRepository.findByMd5Hash(schema.getMd5Hash());
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        // Save new schema
        SchemaEntity saved = schemaRepository.save(schema);
        return saved.getId();
    }

    public Optional<Schema> getSchema(String subject, Integer version) {
        Optional<SubjectVersion> subjectVersion = subjectVersionRepository.findBySubjectAndVersion(SubjectName.of(subject), Version.of(version));
        if (subjectVersion.isEmpty()) {
            return Optional.empty();
        }
        SchemaEntity schemaEntity = schemaRepository.findById(subjectVersion.get().getSchemaId()).orElse(null);
        if (schemaEntity == null) {
            return Optional.empty();
        }
        return Optional.of(Schema.builder()
                .id(schemaEntity.getId().getValue())
                .subject(subject)
                .version(version)
                .schemaText(schemaEntity.getSchemaText())
                .schemaType(schemaEntity.getSchemaType().name())
                .references(schemaEntity.getReferences().stream()
                        .map(ref -> new SchemaReference(ref.getName(), ref.getSubject(), ref.getVersion()))
                        .collect(Collectors.toList()))
                .build());
    }

    public List<Schema> getAllVersions(String subject) {
        List<SubjectVersion> subjectVersions = subjectVersionRepository.findBySubject(SubjectName.of(subject));
        return subjectVersions.stream()
                .map(sv -> {
                    SchemaEntity schemaEntity = schemaRepository.findById(sv.getSchemaId()).orElse(null);
                    if (schemaEntity == null) {
                        return null;
                    }
                    return Schema.builder()
                            .id(schemaEntity.getId().getValue())
                            .subject(subject)
                            .version(sv.getVersion().getValue())
                            .schemaText(schemaEntity.getSchemaText())
                            .schemaType(schemaEntity.getSchemaType().name())
                            .references(schemaEntity.getReferences().stream()
                                    .map(ref -> new SchemaReference(ref.getName(), ref.getSubject(), ref.getVersion()))
                                    .collect(Collectors.toList()))
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Optional<Schema> getLatestSchema(String subject) {
        Optional<SubjectVersion> latestSubjectVersion = subjectVersionRepository.findLatestBySubject(SubjectName.of(subject));
        if (latestSubjectVersion.isEmpty()) {
            return Optional.empty();
        }
        SchemaEntity schemaEntity = schemaRepository.findById(latestSubjectVersion.get().getSchemaId()).orElse(null);
        if (schemaEntity == null) {
            return Optional.empty();
        }
        return Optional.of(Schema.builder()
                .id(schemaEntity.getId().getValue())
                .subject(subject)
                .version(latestSubjectVersion.get().getVersion().getValue())
                .schemaText(schemaEntity.getSchemaText())
                .schemaType(schemaEntity.getSchemaType().name())
                .references(schemaEntity.getReferences().stream()
                        .map(ref -> new SchemaReference(ref.getName(), ref.getSubject(), ref.getVersion()))
                        .collect(Collectors.toList()))
                .build());
    }

    public boolean isCompatible(String subject, String schemaText, String level) {
        org.apache.avro.Schema newSchema;
        try {
            newSchema = new org.apache.avro.Schema.Parser().parse(schemaText);
        } catch (SchemaParseException e) {
            throw new IllegalArgumentException("Invalid Avro schema: " + e.getMessage());
        }

        Optional<Schema> latestOpt = getLatestSchema(subject);
        if (latestOpt.isEmpty()) {
            return true; // no existing, compatible
        }

        Schema latest = latestOpt.get();
        org.apache.avro.Schema existingSchema;
        try {
            existingSchema = new org.apache.avro.Schema.Parser().parse(latest.getSchemaText());
        } catch (SchemaParseException e) {
            throw new IllegalArgumentException("Existing schema is invalid: " + e.getMessage());
        }

        CompatibilityLevel actualLevel = level != null ? CompatibilityLevel.from(level) : getCompatibilityLevel(subject);
        return checkCompatibility(newSchema, existingSchema, actualLevel);
    }

    public void setCompatibilityLevel(String subject, String level) {
        SubjectConfig config = subjectConfigRepository.findBySubject(SubjectName.of(subject)).orElse(new SubjectConfig());
        config.setSubject(SubjectName.of(subject));
        config.setCompatibilityLevel(CompatibilityLevel.from(level));
        subjectConfigRepository.save(config);
    }

    public String getCompatibilityLevelForSubject(String subject) {
        return getCompatibilityLevel(subject).name();
    }

    public List<String> getAllSubjects() {
        return subjectVersionRepository.findAllActiveSubjects();
    }

    public void deleteSchema(String subject, Integer version) {
        Optional<SubjectVersion> subjectVersion = subjectVersionRepository.findBySubjectAndVersion(SubjectName.of(subject), Version.of(version));
        subjectVersion.ifPresent(subjectVersionRepository::delete);
    }

    public void deleteSubject(String subject) {
        List<SubjectVersion> subjectVersions = subjectVersionRepository.findBySubject(SubjectName.of(subject));
        subjectVersionRepository.deleteAll(subjectVersions);
        // Also delete subject config
        subjectConfigRepository.findBySubject(SubjectName.of(subject)).ifPresent(subjectConfigRepository::delete);
    }

    /**
     * Check compatibility between new and existing schemas based on compatibility level.
     *
     * BACKWARD: New schema can read data written with existing schema (consumers can upgrade)
     * FORWARD: Existing schema can read data written with new schema (producers can upgrade)
     * FULL: Both BACKWARD and FORWARD (bidirectional compatibility)
     * NONE: No compatibility checking
     */
    private boolean checkCompatibility(org.apache.avro.Schema newSchema, org.apache.avro.Schema existingSchema, CompatibilityLevel level) {
        switch (level) {
            case NONE:
                return true;

            case BACKWARD:
                // New schema (reader) should be able to read data written with existing schema (writer)
                // Check: can new read old?
                org.apache.avro.SchemaCompatibility.SchemaPairCompatibility backwardCompat =
                    org.apache.avro.SchemaCompatibility.checkReaderWriterCompatibility(newSchema, existingSchema);
                return backwardCompat.getResult().getCompatibility() ==
                    org.apache.avro.SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE;

            case FORWARD:
                // Existing schema (reader) should be able to read data written with new schema (writer)
                // Check: can old read new?
                org.apache.avro.SchemaCompatibility.SchemaPairCompatibility forwardCompat =
                    org.apache.avro.SchemaCompatibility.checkReaderWriterCompatibility(existingSchema, newSchema);
                return forwardCompat.getResult().getCompatibility() ==
                    org.apache.avro.SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE;

            case FULL:
                // Both BACKWARD and FORWARD must be satisfied
                org.apache.avro.SchemaCompatibility.SchemaPairCompatibility fullBackward =
                    org.apache.avro.SchemaCompatibility.checkReaderWriterCompatibility(newSchema, existingSchema);
                org.apache.avro.SchemaCompatibility.SchemaPairCompatibility fullForward =
                    org.apache.avro.SchemaCompatibility.checkReaderWriterCompatibility(existingSchema, newSchema);

                boolean isBackwardCompatible = fullBackward.getResult().getCompatibility() ==
                    org.apache.avro.SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE;
                boolean isForwardCompatible = fullForward.getResult().getCompatibility() ==
                    org.apache.avro.SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE;

                return isBackwardCompatible && isForwardCompatible;

            default:
                // Unknown compatibility level, default to compatible
                return true;
        }
    }
}