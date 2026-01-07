package io.confluent.schemaregistry.pg.api.exception;

import org.springframework.http.HttpStatus;

public class InvalidSchemaException extends SchemaRegistryException {
    public InvalidSchemaException(String message) {
        super(42201, HttpStatus.UNPROCESSABLE_ENTITY, "Invalid Avro schema: " + message);
    }
}
