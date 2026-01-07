package io.confluent.schemaregistry.pg.api.exception;

import org.springframework.http.HttpStatus;

public class SubjectNotFoundException extends SchemaRegistryException {
    public SubjectNotFoundException(String subject) {
        super(40401, HttpStatus.NOT_FOUND, "Subject '" + subject + "' not found");
    }
}
