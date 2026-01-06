package com.example.schemaregistry.domain.model;

import com.example.schemaregistry.domain.value.Md5Hash;
import com.example.schemaregistry.domain.value.SchemaId;
import com.example.schemaregistry.domain.value.SchemaType;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Core schema entity representing a registered schema.
 * Schemas are deduplicated by MD5 hash - identical schemas share the same ID.
 */
@Value
@Builder(toBuilder = true)
@With
public class SchemaEntity {
    /**
     * Globally unique schema ID.
     */
    SchemaId id;

    /**
     * Schema definition text (Avro JSON, JSON Schema, or Protobuf).
     */
    String schemaText;

    /**
     * Schema format type.
     */
    @Builder.Default
    SchemaType schemaType = SchemaType.AVRO;

    /**
     * MD5 hash of schemaText for deduplication.
     */
    Md5Hash md5Hash;

    /**
     * References to other schemas.
     */
    @Builder.Default
    List<SchemaReference> references = List.of();

    /**
     * Optional metadata for governance.
     */
    Metadata metadata;

    /**
     * Optional rule set for transformations/validations.
     */
    RuleSet ruleSet;

    /**
     * When this schema was first registered.
     */
    LocalDateTime createdAt;
}