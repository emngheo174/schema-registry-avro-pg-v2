package com.example.schemaregistry.domain.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

/**
 * Value object representing a schema ID.
 * Schema IDs are globally unique integers starting from 1.
 */
@Value
public class SchemaId {
    int value;

    private SchemaId(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("Schema ID must be positive, got: " + value);
        }
        this.value = value;
    }

    @JsonCreator
    public static SchemaId of(int value) {
        return new SchemaId(value);
    }

    @JsonValue
    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}