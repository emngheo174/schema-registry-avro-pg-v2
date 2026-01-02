package com.example.schemaregistry.service;

import com.example.schemaregistry.model.Schema;
import com.example.schemaregistry.model.SubjectConfig;
import com.example.schemaregistry.repository.SchemaRepository;
import com.example.schemaregistry.repository.SubjectConfigRepository;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.SchemaParseException;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Transactional
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
            org.apache.avro.SchemaCompatibility.SchemaPairCompatibility pair = org.apache.avro.SchemaCompatibility.checkReaderWriterCompatibility(newSchema, existingSchema);
            org.apache.avro.SchemaCompatibility.SchemaCompatibilityResult result = pair.getResult();
            if (!isCompatible(result, level)) {
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
        org.apache.avro.SchemaCompatibility.SchemaPairCompatibility pair = org.apache.avro.SchemaCompatibility.checkReaderWriterCompatibility(newSchema, existingSchema);
        org.apache.avro.SchemaCompatibility.SchemaCompatibilityResult result = pair.getResult();
        return isCompatible(result, actualLevel);
    }

    public void setCompatibilityLevel(String subject, String level) {
        SubjectConfig config = subjectConfigRepository.findBySubject(subject).orElse(new SubjectConfig());
        config.setSubject(subject);
        config.setCompatibilityLevel(level);
        subjectConfigRepository.save(config);
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

    private boolean isCompatible(org.apache.avro.SchemaCompatibility.SchemaCompatibilityResult result, String level) {
        switch (level.toUpperCase()) {
            case "NONE":
                return true;
            case "BACKWARD":
            case "FORWARD":
            case "FULL":
                return result.getCompatibility() == org.apache.avro.SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE;
            default:
                return true; // default to compatible
        }
    }
}