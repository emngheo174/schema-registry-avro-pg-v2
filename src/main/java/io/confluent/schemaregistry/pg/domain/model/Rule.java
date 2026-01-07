package io.confluent.schemaregistry.pg.domain.model;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

/**
 * Individual rule for schema processing (transformation or validation).
 */
@Value
@Builder
public class Rule {
    /**
     * Rule name (unique within ruleset).
     */
    String name;

    /**
     * Optional documentation.
     */
    String doc;

    /**
     * Rule kind: TRANSFORM or CONDITION.
     */
    RuleKind kind;

    /**
     * When to apply: UPGRADE, DOWNGRADE, UPDOWN, WRITE, READ, WRITEREAD.
     */
    RuleMode mode;

    /**
     * Rule type: CEL, CEL_FIELD, JSONATA, ENCRYPT, DECRYPT, REDACT.
     */
    String type;

    /**
     * Tags this rule applies to.
     */
    List<String> tags;

    /**
     * Rule parameters.
     */
    Map<String, String> params;

    /**
     * Rule expression (CEL, JSONata, etc.).
     */
    String expr;

    /**
     * Action on success (optional).
     */
    String onSuccess;

    /**
     * Action on failure: ERROR, NONE, DLQ.
     */
    String onFailure;

    /**
     * Whether this rule is disabled.
     */
    boolean disabled;

    public enum RuleKind {
        TRANSFORM,
        CONDITION
    }

    public enum RuleMode {
        UPGRADE,
        DOWNGRADE,
        UPDOWN,
        WRITE,
        READ,
        WRITEREAD
    }
}
