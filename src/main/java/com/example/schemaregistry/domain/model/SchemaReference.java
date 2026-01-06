package com.example.schemaregistry.domain.model;

import lombok.Value;

/**
 * Reference to another schema.
 */
@Value
public class SchemaReference {
    String name;
    String subject;
    Integer version;
}