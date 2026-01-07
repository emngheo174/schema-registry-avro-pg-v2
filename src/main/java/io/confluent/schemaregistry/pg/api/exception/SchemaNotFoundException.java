package io.confluent.schemaregistry.pg.api.exception;

import org.springframework.http.HttpStatus;

public class SchemaNotFoundException extends SchemaRegistryException {
    public SchemaNotFoundException(int schemaId) {
        super(40403, HttpStatus.NOT_FOUND, "Schema " + schemaId + " not found");
    }
}
