package io.confluent.schemaregistry.pg.domain.model;

import io.confluent.schemaregistry.pg.domain.value.CompatibilityLevel;
import io.confluent.schemaregistry.pg.domain.value.Mode;
import io.confluent.schemaregistry.pg.domain.value.SubjectName;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;

/**
 * Subject-level configuration.
 * If not set, inherits from global config.
 */
@Value
@Builder
@With
public class SubjectConfig {
    SubjectName subject;

    CompatibilityLevel compatibility;

    String compatibilityGroup;

    Mode mode;

    String alias;

    @Builder.Default
    boolean normalize = false;

    Metadata defaultMetadata;

    Metadata overrideMetadata;

    RuleSet defaultRuleSet;

    RuleSet overrideRuleSet;

    Instant updatedAt;
}
