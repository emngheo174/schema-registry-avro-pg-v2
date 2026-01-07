package io.confluent.schemaregistry.pg.domain.service;

import io.confluent.schemaregistry.pg.domain.value.SchemaType;
import org.springframework.stereotype.Service;

/**
 * Service for normalizing schemas.
 */
@Service
public class NormalizationService {

    public String normalize(String schemaText, SchemaType type) {
        // TODO: Implement Avro schema normalization (canonical form)
        // For now, return as-is
        return schemaText;
    }
}
