package io.confluent.schemaregistry.pg.domain.value;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Value;

/**
 * Value object representing a schema registry context for multi-tenancy.
 * Format: ":.contextName.:" or "." for default context.
 */
@Value
public class Context {
    private static final Context DEFAULT = new Context(".");

    @JsonValue
    String value;

    private Context(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Context cannot be null or blank");
        }
        if (!value.equals(".") && (!value.startsWith(":.") || !value.endsWith(".:"))) {
            throw new IllegalArgumentException(
                "Context must be '.' (default) or format ':.name.:', got: " + value
            );
        }
        this.value = value;
    }

    @JsonCreator
    public static Context of(String value) {
        return new Context(value);
    }

    public static Context defaultContext() {
        return DEFAULT;
    }

    public boolean isDefault() {
        return ".".equals(value);
    }

    /**
     * Extracts the context name without delimiters.
     * For ":.tenant1.:" returns "tenant1"
     * For "." returns ""
     */
    public String getName() {
        if (isDefault()) {
            return "";
        }
        return value.substring(2, value.length() - 2);
    }

    @Override
    public String toString() {
        return value;
    }
}
