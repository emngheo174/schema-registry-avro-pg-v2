package io.confluent.schemaregistry.pg.api.exception;

import org.springframework.http.HttpStatus;

public class VersionNotFoundException extends SchemaRegistryException {
    public VersionNotFoundException(String subject, int version) {
        super(40402, HttpStatus.NOT_FOUND,
            "Version " + version + " not found for subject '" + subject + "'");
    }
}
