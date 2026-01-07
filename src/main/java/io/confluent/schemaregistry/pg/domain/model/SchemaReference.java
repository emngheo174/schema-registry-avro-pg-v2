package io.confluent.schemaregistry.pg.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.confluent.schemaregistry.pg.domain.value.SubjectName;
import io.confluent.schemaregistry.pg.domain.value.Version;
import lombok.Builder;
import lombok.Value;

/**
 * Reference to another schema for composition.
 * Used in Avro named types.
 */
@Value
@Builder
public class SchemaReference {
    /**
     * Reference name used in the schema (e.g., fully qualified type name).
     */
    String name;

    /**
     * Subject containing the referenced schema.
     */
    SubjectName subject;

    /**
     * Version of the referenced schema (-1 for latest).
     */
    Version version;

    @JsonCreator
    public static SchemaReference fromJson(
            @JsonProperty("name") String name,
            @JsonProperty("subject") String subject,
            @JsonProperty("version") Integer version) {
        return SchemaReference.builder()
                .name(name)
                .subject(SubjectName.of(subject))
                .version(Version.of(version))
                .build();
    }
}
