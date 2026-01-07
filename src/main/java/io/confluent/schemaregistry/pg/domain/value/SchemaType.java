package io.confluent.schemaregistry.pg.domain.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported schema format types.
 * This implementation only supports AVRO.
 */
public enum SchemaType {
    AVRO("Apache Avro schema format");

    private final String description;

    SchemaType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    @JsonValue
    public String toValue() {
        return name();
    }

    /**
     * Parse schema type from string, case-insensitive.
     * Only AVRO is supported.
     */
    @JsonCreator
    public static SchemaType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Schema type cannot be null or blank");
        }
        if (!"AVRO".equalsIgnoreCase(value)) {
            throw new IllegalArgumentException(
                "Invalid schema type: " + value + ". Only AVRO is supported"
            );
        }
        return AVRO;
    }
}
