package io.confluent.schemaregistry.pg.api.controller;

import io.confluent.schemaregistry.pg.domain.model.SchemaEntity;
import io.confluent.schemaregistry.pg.domain.service.SchemaService;
import io.confluent.schemaregistry.pg.domain.value.SchemaId;
import io.confluent.schemaregistry.pg.domain.value.SubjectName;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for /schemas endpoints.
 */
@RestController
@RequestMapping("/schemas")
public class SchemasController {

    private final SchemaService schemaService;

    public SchemasController(SchemaService schemaService) {
        this.schemaService = schemaService;
    }

    /**
     * GET /schemas/ids/{id} - Get schema by global ID.
     */
    @GetMapping("/ids/{id}")
    public ResponseEntity<Map<String, Object>> getSchemaById(
            @PathVariable int id,
            @RequestParam(defaultValue = "false") boolean fetchMaxId
    ) {
        SchemaId schemaId = SchemaId.of(id);
        SchemaEntity schema = schemaService.getById(schemaId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("schema", schema.getSchemaText());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /schemas/ids/{id}/schema - Get raw schema text by ID.
     */
    @GetMapping("/ids/{id}/schema")
    public ResponseEntity<String> getSchemaStringById(@PathVariable int id) {
        SchemaId schemaId = SchemaId.of(id);
        SchemaEntity schema = schemaService.getById(schemaId);
        return ResponseEntity.ok(schema.getSchemaText());
    }

    /**
     * GET /schemas - List all schemas.
     */
    @GetMapping
    public ResponseEntity<List<Integer>> listSchemas(
            @RequestParam(required = false) String subjectPrefix,
            @RequestParam(defaultValue = "false") boolean deleted,
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        SubjectName subjectFilter = subjectPrefix != null ? SubjectName.of(subjectPrefix) : null;
        List<SchemaId> schemaIds = schemaService.listAllSchemaIds(subjectFilter, deleted, limit, offset);
        List<Integer> ids = schemaIds.stream().map(SchemaId::getValue).toList();
        return ResponseEntity.ok(ids);
    }
}
