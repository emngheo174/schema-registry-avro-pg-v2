package com.example.schemaregistry.domain.model;

import lombok.Value;

import java.util.Map;

/**
 * Metadata for schema governance.
 */
@Value
public class Metadata {
    Map<String, Object> properties;
    Map<String, String> tags;
}