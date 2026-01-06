package com.example.schemaregistry.domain.model;

import lombok.Value;

import java.util.List;

/**
 * Rule set for schema transformations and validations.
 */
@Value
public class RuleSet {
    List<Rule> migrationRules;
    List<Rule> domainRules;
}