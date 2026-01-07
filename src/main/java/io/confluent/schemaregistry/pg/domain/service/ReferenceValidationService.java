package io.confluent.schemaregistry.pg.domain.service;

import io.confluent.schemaregistry.pg.domain.model.SchemaReference;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for validating schema references.
 */
@Service
public class ReferenceValidationService {

    public List<String> validateReferences(List<SchemaReference> references) {
        // TODO: Implement reference validation
        // Check that referenced subject-versions exist
        List<String> errors = new ArrayList<>();
        return errors;
    }
}
