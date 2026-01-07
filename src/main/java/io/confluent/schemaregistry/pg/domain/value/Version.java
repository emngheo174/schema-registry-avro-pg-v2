package io.confluent.schemaregistry.pg.domain.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

/**
 * Value object representing a schema version.
 * Can be an integer version number, "latest", or -1 (equivalent to latest).
 */
@Value
public class Version {
    private static final String LATEST_KEYWORD = "latest";
    private static final int LATEST_VALUE = -1;

    @JsonValue
    int value;

    private Version(int value) {
        if (value < -1 || value == 0) {
            throw new IllegalArgumentException(
                "Version must be positive or -1 for latest, got: " + value
            );
        }
        this.value = value;
    }

    @JsonCreator
    public static Version of(int value) {
        return new Version(value);
    }

    public static Version of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Version string cannot be null or blank");
        }
        if (LATEST_KEYWORD.equalsIgnoreCase(value)) {
            return latest();
        }
        try {
            return of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "Version must be an integer or 'latest', got: " + value, e
            );
        }
    }

    public static Version latest() {
        return new Version(LATEST_VALUE);
    }

    public boolean isLatest() {
        return value == LATEST_VALUE;
    }

    @Override
    public String toString() {
        return isLatest() ? LATEST_KEYWORD : String.valueOf(value);
    }
}
