package com.example.schemaregistry.service;

import com.example.schemaregistry.model.Schema;
import com.example.schemaregistry.repository.SchemaRepository;
import org.apache.avro.SchemaParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SchemaService {

    @Autowired
    private SchemaRepository schemaRepository;

    public Schema registerSchema(String subject, String schemaText) {
        // Validate Avro schema
        try {
            new org.apache.avro.Schema.Parser().parse(schemaText);
        } catch (SchemaParseException e) {
            throw new IllegalArgumentException("Invalid Avro schema: " + e.getMessage());
        }

        // Get next version
        List<Schema> existing = schemaRepository.findBySubject(subject);
        int nextVersion = existing.stream().mapToInt(Schema::getVersion).max().orElse(0) + 1;

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
}