package io.confluent.schemaregistry.pg.api.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base exception for all Schema Registry errors.
 * Each exception has an error code matching Confluent's specification.
 */
@Getter
public abstract class SchemaRegistryException extends RuntimeException {
    private final int errorCode;
    private final HttpStatus httpStatus;

    protected SchemaRegistryException(int errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected SchemaRegistryException(int errorCode, HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
