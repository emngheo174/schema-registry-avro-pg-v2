package io.confluent.schemaregistry.pg.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Collection of rules for schema processing.
 */
@Value
@Builder
public class RuleSet {
    /**
     * Migration rules for schema evolution (UPGRADE/DOWNGRADE).
     */
    List<Rule> migrationRules;

    /**
     * Domain rules for business validation (WRITE/READ).
     */
    List<Rule> domainRules;
}
