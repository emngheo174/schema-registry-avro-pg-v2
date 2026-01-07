package io.confluent.schemaregistry.pg.api.exception;

import org.springframework.http.HttpStatus;

public class IncompatibleSchemaException extends SchemaRegistryException {
    public IncompatibleSchemaException(String subject, String compatibilityLevel, String reason) {
        super(409, HttpStatus.CONFLICT,
            "Schema is incompatible with subject '" + subject +
            "' (level: " + compatibilityLevel + "): " + reason);
    }
}
