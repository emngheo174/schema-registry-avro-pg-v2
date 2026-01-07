package io.confluent.schemaregistry.pg.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Schema metadata for governance and tagging.
 * Supports PII tagging, sensitive field marking, and custom properties.
 */
@Value
@Builder
public class Metadata {
    /**
     * Field tags mapped by tag name to field paths.
     * Example: {"PII": ["$.email", "$.ssn"], "SENSITIVE": ["$.password"]}
     */
    Map<String, List<String>> tags;

    /**
     * Custom properties for governance.
     * Example: {"owner": "team-data", "domain": "identity"}
     */
    Map<String, String> properties;

    /**
     * Sensitive field paths requiring special handling.
     * Example: ["$.password", "$.apiKey"]
     */
    List<String> sensitive;
}
