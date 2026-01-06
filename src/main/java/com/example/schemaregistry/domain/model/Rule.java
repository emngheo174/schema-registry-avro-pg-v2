package com.example.schemaregistry.domain.model;

import lombok.Value;

/**
 * Individual rule for schema transformations/validations.
 */
@Value
public class Rule {
    String name;
    String doc;
    String kind;
    String mode;
    String type;
    Map<String, Object> params;
    String expr;
    boolean disabled;
}