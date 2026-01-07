package io.confluent.schemaregistry.pg.domain.value;

/**
 * Schema compatibility levels defining evolution rules.
 */
public enum CompatibilityLevel {
    NONE(
        "No compatibility checking",
        false, false, false
    ),
    BACKWARD(
        "New schema can read data written by last schema version",
        true, false, false
    ),
    BACKWARD_TRANSITIVE(
        "New schema can read data written by all previous schema versions",
        true, false, true
    ),
    FORWARD(
        "Last schema version can read data written by new schema",
        false, true, false
    ),
    FORWARD_TRANSITIVE(
        "All previous schema versions can read data written by new schema",
        false, true, true
    ),
    FULL(
        "Both backward and forward compatible with last version",
        true, true, false
    ),
    FULL_TRANSITIVE(
        "Both backward and forward compatible with all versions",
        true, true, true
    );

    private final String description;
    private final boolean backward;
    private final boolean forward;
    private final boolean transitive;

    CompatibilityLevel(String description, boolean backward, boolean forward, boolean transitive) {
        this.description = description;
        this.backward = backward;
        this.forward = forward;
        this.transitive = transitive;
    }

    public String getDescription() {
        return description;
    }

    public boolean isBackward() {
        return backward;
    }

    public boolean isForward() {
        return forward;
    }

    public boolean isTransitive() {
        return transitive;
    }

    /**
     * Parse compatibility level from string, case-insensitive.
     */
    public static CompatibilityLevel from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Compatibility level cannot be null or blank");
        }
        try {
            return CompatibilityLevel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid compatibility level: " + value +
                ". Supported: NONE, BACKWARD, BACKWARD_TRANSITIVE, FORWARD, FORWARD_TRANSITIVE, FULL, FULL_TRANSITIVE",
                e
            );
        }
    }
}
