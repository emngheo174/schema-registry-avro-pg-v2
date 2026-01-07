package io.confluent.schemaregistry.pg.api.exception;

import org.springframework.http.HttpStatus;
import java.util.List;

public class InvalidReferenceException extends SchemaRegistryException {
    public InvalidReferenceException(List<String> errors) {
        super(42202, HttpStatus.UNPROCESSABLE_ENTITY,
            "Invalid schema references: " + String.join(", ", errors));
    }
}
