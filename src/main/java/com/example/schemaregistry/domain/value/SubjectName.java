package com.example.schemaregistry.domain.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

/**
 * Value object representing a subject name.
 * Subjects are named scopes under which schemas are versioned.
 * Can include context prefix (e.g., ":.tenant1.:my-topic-value")
 */
@Value
public class SubjectName {
    @JsonValue
    String value;

    private SubjectName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Subject name cannot be null or blank");
        }
        if (value.length() > 255) {
            throw new IllegalArgumentException("Subject name cannot exceed 255 characters");
        }
        this.value = value;
    }

    @JsonCreator
    public static SubjectName of(String value) {
        return new SubjectName(value);
    }

    /**
     * Checks if this subject belongs to a non-default context.
     * Context format: ":.contextName.:subjectName"
     */
    public boolean hasContext() {
        return value.startsWith(":.") && value.indexOf(".:") > 2;
    }

    /**
     * Extracts the context from this subject name, if present.
     * Returns the default context (".") if no context prefix.
     */
    public Context extractContext() {
        if (!hasContext()) {
            return Context.defaultContext();
        }
        int endIndex = value.indexOf(".:") + 2;
        return Context.of(value.substring(0, endIndex));
    }

    @Override
    public String toString() {
        return value;
    }
}