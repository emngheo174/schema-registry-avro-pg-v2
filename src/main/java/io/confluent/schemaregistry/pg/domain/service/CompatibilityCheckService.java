package io.confluent.schemaregistry.pg.domain.service;

import io.confluent.schemaregistry.pg.domain.value.CompatibilityLevel;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference;
import org.apache.avro.Schema;
import org.apache.avro.SchemaCompatibility;
import org.apache.avro.SchemaCompatibility.SchemaPairCompatibility;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for checking Avro schema compatibility.
 */
@Service
public class CompatibilityCheckService {

    /**
     * Check if newSchema is compatible with existing schemas according to compatibility level.
     *
     * @param newSchemaText     New schema to validate
     * @param existingSchemas   Existing schemas (ordered from newest to oldest)
     * @param compatibilityLevel Compatibility level to enforce
     * @return List of compatibility errors (empty if compatible)
     */
    public List<String> checkCompatibility(
            String newSchemaText,
            List<String> existingSchemas,
            CompatibilityLevel compatibilityLevel
    ) {
        if (compatibilityLevel == CompatibilityLevel.NONE) {
            return Collections.emptyList();
        }

        if (existingSchemas == null || existingSchemas.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Schema newSchema = new Schema.Parser().parse(newSchemaText);

            // Determine how many schemas to check based on transitive flag
            List<String> schemasToCheck = compatibilityLevel.isTransitive()
                    ? existingSchemas
                    : List.of(existingSchemas.get(0)); // Only check against latest

            List<String> errors = new ArrayList<>();

            for (String existingSchemaText : schemasToCheck) {
                Schema existingSchema = new Schema.Parser().parse(existingSchemaText);

                if (compatibilityLevel.isBackward() || compatibilityLevel == CompatibilityLevel.FULL || compatibilityLevel == CompatibilityLevel.FULL_TRANSITIVE) {
                    // Check backward compatibility: new schema can read data written with old schema
                    SchemaPairCompatibility backwardResult = SchemaCompatibility.checkReaderWriterCompatibility(
                            newSchema, existingSchema
                    );
                    if (backwardResult.getType() != SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE) {
                        errors.add("Backward compatibility check failed: " + backwardResult.getDescription());
                    }
                }

                if (compatibilityLevel.isForward() || compatibilityLevel == CompatibilityLevel.FULL || compatibilityLevel == CompatibilityLevel.FULL_TRANSITIVE) {
                    // Check forward compatibility: old schema can read data written with new schema
                    SchemaPairCompatibility forwardResult = SchemaCompatibility.checkReaderWriterCompatibility(
                            existingSchema, newSchema
                    );
                    if (forwardResult.getType() != SchemaCompatibility.SchemaCompatibilityType.COMPATIBLE) {
                        errors.add("Forward compatibility check failed: " + forwardResult.getDescription());
                    }
                }
            }

            return errors;

        } catch (Exception e) {
            return List.of("Failed to parse schema for compatibility check: " + e.getMessage());
        }
    }

    /**
     * Check if two schemas are identical.
     */
    public boolean areSchemasIdentical(String schema1, String schema2) {
        try {
            Schema s1 = new Schema.Parser().parse(schema1);
            Schema s2 = new Schema.Parser().parse(schema2);
            return s1.equals(s2);
        } catch (Exception e) {
            return false;
        }
    }
}
