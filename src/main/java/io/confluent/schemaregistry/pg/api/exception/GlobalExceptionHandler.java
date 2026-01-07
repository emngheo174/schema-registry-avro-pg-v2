package io.confluent.schemaregistry.pg.api.exception;

import io.confluent.schemaregistry.pg.api.dto.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Global exception handler for all Schema Registry exceptions.
 * Converts exceptions to ErrorResponse format matching Confluent's API.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handle all SchemaRegistryException instances.
     */
    @ExceptionHandler(SchemaRegistryException.class)
    public ResponseEntity<ErrorResponse> handleSchemaRegistryException(
            SchemaRegistryException ex,
            WebRequest request
    ) {
        log.debug("Schema Registry exception: {} - {}", ex.getErrorCode(), ex.getMessage());
        ErrorResponse error = ErrorResponse.of(ex.getErrorCode(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    /**
     * Handle IllegalArgumentException (typically from value object validation).
     * Map to 422 Unprocessable Entity.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request
    ) {
        log.debug("Illegal argument exception: {}", ex.getMessage());
        ErrorResponse error = ErrorResponse.of(42201, ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
    }

    /**
     * Handle all other unexpected exceptions.
     * Map to 500 Internal Server Error.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request
    ) {
        log.error("Unexpected error", ex);
        ErrorResponse error = ErrorResponse.of(50001, "Internal server error: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
