package io.confluent.schemaregistry.pg.domain.service;

import io.confluent.schemaregistry.pg.api.exception.SchemaNotFoundException;
import io.confluent.schemaregistry.pg.domain.model.SchemaEntity;
import io.confluent.schemaregistry.pg.domain.model.SchemaReference;
import io.confluent.schemaregistry.pg.domain.value.Md5Hash;
import io.confluent.schemaregistry.pg.domain.value.SchemaId;
import io.confluent.schemaregistry.pg.domain.value.SubjectName;
import io.confluent.schemaregistry.pg.domain.value.Version;
import io.confluent.schemaregistry.pg.infrastructure.persistence.SchemaReferenceRepository;
import io.confluent.schemaregistry.pg.infrastructure.persistence.SchemaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for schema operations.
 */
@Service
public class SchemaService {

    private final SchemaRepository schemaRepository;
    private final SchemaReferenceRepository schemaReferenceRepository;

    public SchemaService(SchemaRepository schemaRepository,
                         SchemaReferenceRepository schemaReferenceRepository) {
        this.schemaRepository = schemaRepository;
        this.schemaReferenceRepository = schemaReferenceRepository;
    }

    public SchemaEntity getById(SchemaId schemaId) {
        Optional<SchemaEntity> schema = schemaRepository.findById(schemaId);
        if (schema.isEmpty()) {
            throw new SchemaNotFoundException(schemaId.getValue());
        }

        SchemaEntity schemaEntity = schema.get();

        // Load references
        List<SchemaReference> references = schemaReferenceRepository.findBySchemaId(schemaId);

        return schemaEntity.withReferences(references);
    }

    @Transactional
    public SchemaEntity registerOrGetExisting(SchemaEntity schema, Integer explicitId) {
        Md5Hash hash = Md5Hash.compute(schema.getSchemaText());

        // Check if schema already exists
        Optional<SchemaEntity> existing = schemaRepository.findByHash(hash);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Register new schema
        SchemaEntity schemaWithHash = schema.withMd5Hash(hash).withCreatedAt(Instant.now());

        SchemaEntity saved = explicitId != null
                ? schemaRepository.saveWithId(schemaWithHash, explicitId)
                : schemaRepository.save(schemaWithHash);

        // Save references if any
        if (schema.getReferences() != null && !schema.getReferences().isEmpty()) {
            schemaReferenceRepository.saveAll(saved.getId(), schema.getReferences());
        }

        return saved;
    }

    public List<Integer> getSchemaIdsReferencingSubjectVersion(SubjectName subjectName, Version version) {
        return schemaReferenceRepository.findSchemaIdsReferencingSubjectVersion(subjectName, version);
    }

    public List<SchemaId> listAllSchemaIds(SubjectName subjectFilter, boolean deleted, int limit, int offset) {
        // TODO: Implement filtering by subject and deleted status
        return schemaRepository.findAllIds(limit, offset);
    }

    public Optional<SchemaEntity> findByHash(Md5Hash hash) {
        return schemaRepository.findByHash(hash);
    }
}
