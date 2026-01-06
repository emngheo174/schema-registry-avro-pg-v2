package com.example.schemaregistry.domain.value;

/**
 * Operational modes for schema registry.
 */
public enum Mode {
    READWRITE(
        "Normal operation - read and write allowed",
        true, true, false
    ),
    READONLY(
        "Read-only mode - writes rejected",
        true, false, false
    ),
    READONLY_OVERRIDE(
        "Read-only with override capability",
        true, true, false
    ),
    IMPORT(
        "Import mode - allows explicit schema IDs and bypasses compatibility checks",
        true, true, true
    ),
    FORWARD(
        "Forward writes to leader (used in distributed setups)",
        true, true, false
    );

    private final String description;
    private final boolean readAllowed;
    private final boolean writeAllowed;
    private final boolean importMode;

    Mode(String description, boolean readAllowed, boolean writeAllowed, boolean importMode) {
        this.description = description;
        this.readAllowed = readAllowed;
        this.writeAllowed = writeAllowed;
        this.importMode = importMode;
    }

    public String getDescription() {
        return description;
    }

    public boolean isReadAllowed() {
        return readAllowed;
    }

    public boolean isWriteAllowed() {
        return writeAllowed;
    }

    public boolean isImportMode() {
        return importMode;
    }

    /**
     * Parse mode from string, case-insensitive.
     */
    public static Mode from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Mode cannot be null or blank");
        }
        try {
            return Mode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid mode: " + value +
                ". Supported: READWRITE, READONLY, READONLY_OVERRIDE, IMPORT, FORWARD",
                e
            );
        }
    }
}