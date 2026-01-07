package io.confluent.schemaregistry.pg.domain.model;

import io.confluent.schemaregistry.pg.domain.value.SchemaId;
import io.confluent.schemaregistry.pg.domain.value.SubjectName;
import io.confluent.schemaregistry.pg.domain.value.Version;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;

/**
 * Mapping between a subject, version number, and schema ID.
 * Multiple subjects can reference the same schema ID (deduplication).
 */
@Value
@Builder
@With
public class SubjectVersion {
    /**
     * Internal database ID.
     */
    Long id;

    /**
     * Subject name (may include context prefix).
     */
    SubjectName subject;

    /**
     * Version number within this subject.
     */
    Version version;

    /**
     * Reference to the schema.
     */
    SchemaId schemaId;

    /**
     * Soft delete flag.
     */
    boolean deleted;

    /**
     * When this version was registered.
     */
    Instant createdAt;
}
