package com.example.schemaregistry.service;

import com.example.schemaregistry.model.Schema;
import com.example.schemaregistry.model.SubjectConfig;
import com.example.schemaregistry.repository.SchemaRepository;
import com.example.schemaregistry.repository.SubjectConfigRepository;
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

    @Autowired
    private SchemaRepository schemaRepository;

    @Autowired
    private SubjectConfigRepository subjectConfigRepository;

    @Value("${schema.registry.compatibility.level:BACKWARD}")
    private String compatibilityLevel;

    private String getCompatibilityLevel(String subject) {
        return subjectConfigRepository.findBySubject(subject)
                .map(SubjectConfig::getCompatibilityLevel)
                .orElse(compatibilityLevel);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Schema registerSchema(String subject, String schemaText) {
        // Validate Avro schema
        org.apache.avro.Schema newSchema;
        try {
            newSchema = new org.apache.avro.Schema.Parser().parse(schemaText);
        } catch (SchemaParseException e) {
            throw new IllegalArgumentException("Invalid Avro schema: " + e.getMessage());
        }

        // Get existing schemas with lock
        Optional<Schema> latestOpt = schemaRepository.findLatestBySubjectForUpdate(subject);

        // Check compatibility if there are existing schemas
        if (latestOpt.isPresent()) {
            Schema latest = latestOpt.get();
            org.apache.avro.Schema existingSchema;
            try {
                existingSchema = new org.apache.avro.Schema.Parser().parse(latest.getSchemaText());
            } catch (SchemaParseException e) {
                throw new IllegalArgumentException("Existing schema is invalid: " + e.getMessage());
            }

            String level = getCompatibilityLevel(subject);
            if (!checkCompatibility(newSchema, existingSchema, level)) {
                throw new IllegalArgumentException("Schema is not compatible with existing schema. Compatibility level: " + level);
            }
        }

        // Get next version
        int nextVersion = latestOpt.map(s -> s.getVersion() + 1).orElse(1);

        Schema schema = new Schema();
        schema.setSubject(subject);
        schema.setVersion(nextVersion);
        schema.setSchemaText(schemaText);
        schema.setSchemaType("AVRO");

        return schemaRepository.save(schema);
    }

    public Optional<Schema> getSchema(String subject, Integer version) {
        return schemaRepository.findBySubjectAndVersion(subject, version);
    }

    public List<Schema> getAllVersions(String subject) {
        return schemaRepository.findBySubject(subject);
    }

    public Optional<Schema> getLatestSchema(String subject) {
        return schemaRepository.findLatestBySubject(subject);
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

        String actualLevel = level != null ? level : getCompatibilityLevel(subject);
        return checkCompatibility(newSchema, existingSchema, actualLevel);
    }

    public void setCompatibilityLevel(String subject, String level) {
        SubjectConfig config = subjectConfigRepository.findBySubject(subject).orElse(new SubjectConfig());
        config.setSubject(subject);
        config.setCompatibilityLevel(level);
        subjectConfigRepository.save(config);
    }

    public String getCompatibilityLevelForSubject(String subject) {
        return getCompatibilityLevel(subject);
    }

    public List<String> getAllSubjects() {
        return schemaRepository.findAllSubjects();
    }

    public void deleteSchema(String subject, Integer version) {
        Optional<Schema> schema = schemaRepository.findBySubjectAndVersion(subject, version);
        schema.ifPresent(schemaRepository::delete);
    }

    public void deleteSubject(String subject) {
        List<Schema> schemas = schemaRepository.findBySubject(subject);
        schemaRepository.deleteAll(schemas);
        // Also delete subject config
        subjectConfigRepository.findBySubject(subject).ifPresent(subjectConfigRepository::delete);
    }

    /**
     * Check compatibility between new and existing schemas based on compatibility level.
     *
     * BACKWARD: New schema can read data written with existing schema (consumers can upgrade)
     * FORWARD: Existing schema can read data written with new schema (producers can upgrade)
     * FULL: Both BACKWARD and FORWARD (bidirectional compatibility)
     * NONE: No compatibility checking
     */
    private boolean checkCompatibility(org.apache.avro.Schema newSchema, org.apache.avro.Schema existingSchema, String level) {
        switch (level.toUpperCase()) {
            case "NONE":
                return true;

            case "BACKWARD":
                // New schema (reader) should be able to read data written with existing schema (writer)
                // Check: can new read old?
                org.apache.avro.SchemaCompatibility.SchemaPairCompatibility backwardCompat =
                    org.apache.avro.SchemaCompatibility.checkReaderWriterCompatibility(newSchema, existingSchema);
                return backwardCompat.getResult().getCompatibility() ==
                    org.apache.avro.SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE;

            case "FORWARD":
                // Existing schema (reader) should be able to read data written with new schema (writer)
                // Check: can old read new?
                org.apache.avro.SchemaCompatibility.SchemaPairCompatibility forwardCompat =
                    org.apache.avro.SchemaCompatibility.checkReaderWriterCompatibility(existingSchema, newSchema);
                return forwardCompat.getResult().getCompatibility() ==
                    org.apache.avro.SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE;

            case "FULL":
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