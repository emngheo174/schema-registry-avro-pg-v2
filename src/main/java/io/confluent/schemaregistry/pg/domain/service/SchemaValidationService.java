package io.confluent.schemaregistry.pg.domain.service;

import io.confluent.schemaregistry.pg.api.exception.InvalidSchemaException;
import io.confluent.schemaregistry.pg.domain.value.SchemaType;
import org.apache.avro.Schema;
import org.springframework.stereotype.Service;

/**
 * Service for validating schema syntax.
 */
@Service
public class SchemaValidationService {

    public void validate(String schemaText, SchemaType type) {
        if (type != SchemaType.AVRO) {
            throw new IllegalArgumentException("Only AVRO schema type is supported");
        }

        try {
            new Schema.Parser().parse(schemaText);
        } catch (Exception e) {
            throw new InvalidSchemaException(e.getMessage());
        }
    }
}
