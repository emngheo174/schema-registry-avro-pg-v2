package io.confluent.schemaregistry.pg.domain.model;

import io.confluent.schemaregistry.pg.domain.value.CompatibilityLevel;
import io.confluent.schemaregistry.pg.domain.value.Mode;
import lombok.Builder;
import lombok.Value;
import lombok.With;

import java.time.Instant;

/**
 * Global default configuration.
 */
@Value
@Builder
@With
public class GlobalConfig {
    @Builder.Default
    CompatibilityLevel compatibility = CompatibilityLevel.BACKWARD;

    @Builder.Default
    Mode mode = Mode.READWRITE;

    String compatibilityGroup;

    Metadata defaultMetadata;

    Metadata overrideMetadata;

    RuleSet defaultRuleSet;

    RuleSet overrideRuleSet;

    Instant updatedAt;
}
