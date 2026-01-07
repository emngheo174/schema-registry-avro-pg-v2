package io.confluent.schemaregistry.pg.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.confluent.schemaregistry.pg.domain.value.Md5Hash;
import io.confluent.schemaregistry.pg.domain.value.SchemaId;
import io.confluent.schemaregistry.pg.domain.value.SchemaType;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;
import java.util.List;

/**
 * Core schema entity representing a registered schema.
 * Schemas are deduplicated by MD5 hash - identical schemas share the same ID.
 */
@Value
@Builder(toBuilder = true)
@With
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SchemaEntity {
    /**
     * Globally unique schema ID.
     */
    @JsonProperty("id")
    SchemaId id;

    /**
     * Schema definition text (Avro JSON only).
     */
    @JsonProperty("schema")
    String schemaText;

    /**
     * Schema format type (AVRO only).
     */
    @JsonProperty("schemaType")
    SchemaType schemaType;

    /**
     * MD5 hash of schemaText for deduplication.
     */
    @JsonIgnore
    Md5Hash md5Hash;

    /**
     * References to other schemas.
     */
    @JsonProperty("references")
    @Builder.Default
    List<SchemaReference> references = List.of();

    /**
     * Optional metadata for governance.
     */
    @JsonProperty("metadata")
    Metadata metadata;

    /**
     * Optional rule set for transformations/validations.
     */
    @JsonProperty("ruleSet")
    RuleSet ruleSet;

    /**
     * When this schema was first registered.
     */
    @JsonIgnore
    Instant createdAt;

    /**
     * Factory method for JSON deserialization.
     */
    @JsonCreator
    public static SchemaEntity fromJson(
            @JsonProperty("id") Integer id,
            @JsonProperty("schema") String schema,
            @JsonProperty("schemaType") String schemaType,
            @JsonProperty("references") List<SchemaReference> references,
            @JsonProperty("metadata") Metadata metadata,
            @JsonProperty("ruleSet") RuleSet ruleSet) {
        return SchemaEntity.builder()
                .id(id != null ? SchemaId.of(id) : null)
                .schemaText(schema)
                .schemaType(schemaType != null ? SchemaType.from(schemaType) : SchemaType.AVRO)
                .references(references != null ? references : List.of())
                .metadata(metadata)
                .ruleSet(ruleSet)
                .build();
    }
}
