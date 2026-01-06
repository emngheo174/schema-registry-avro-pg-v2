package com.example.schemaregistry.domain.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Supported schema format types.
 */
public enum SchemaType {
    AVRO("Apache Avro schema format"),
    JSON("JSON Schema format"),
    PROTOBUF("Protocol Buffers schema format");

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
     */
    @JsonCreator
    public static SchemaType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Schema type cannot be null or blank");
        }
        try {
            return SchemaType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid schema type: " + value + ". Supported types: AVRO, JSON, PROTOBUF", e
            );
        }
    }
}