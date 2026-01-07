package io.confluent.schemaregistry.pg.api.controller;

import io.confluent.schemaregistry.pg.api.exception.InvalidReferenceException;
import io.confluent.schemaregistry.pg.domain.model.SchemaEntity;
import io.confluent.schemaregistry.pg.domain.model.SchemaReference;
import io.confluent.schemaregistry.pg.domain.model.SubjectVersion;
import io.confluent.schemaregistry.pg.domain.service.NormalizationService;
import io.confluent.schemaregistry.pg.domain.service.ReferenceValidationService;
import io.confluent.schemaregistry.pg.domain.service.SchemaService;
import io.confluent.schemaregistry.pg.domain.service.SchemaValidationService;
import io.confluent.schemaregistry.pg.domain.service.SubjectService;
import io.confluent.schemaregistry.pg.domain.value.SchemaId;
import io.confluent.schemaregistry.pg.domain.value.SchemaType;
import io.confluent.schemaregistry.pg.domain.value.SubjectName;
import io.confluent.schemaregistry.pg.domain.value.Version;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for /subjects/{subject}/versions endpoints.
 */
@RestController
@RequestMapping("/subjects/{subject}/versions")
public class SubjectVersionsController {

    private final SubjectService subjectService;
    private final SchemaService schemaService;
    private final NormalizationService normalizationService;
    private final SchemaValidationService schemaValidationService;
    private final ReferenceValidationService referenceValidationService;

    public SubjectVersionsController(SubjectService subjectService,
                                     SchemaService schemaService,
                                     NormalizationService normalizationService,
                                     SchemaValidationService schemaValidationService,
                                     ReferenceValidationService referenceValidationService) {
        this.subjectService = subjectService;
        this.schemaService = schemaService;
        this.normalizationService = normalizationService;
        this.schemaValidationService = schemaValidationService;
        this.referenceValidationService = referenceValidationService;
    }

    /**
     * GET /subjects/{subject}/versions - List all versions.
     */
    @GetMapping
    public ResponseEntity<List<Integer>> listVersions(
            @PathVariable String subject,
            @RequestParam(defaultValue = "false") boolean deleted
    ) {
        SubjectName subjectName = SubjectName.of(subject);
        List<Integer> versions = subjectService.listVersions(subjectName, deleted);
        return ResponseEntity.ok(versions);
    }

    /**
     * POST /subjects/{subject}/versions - Register new schema.
     */
    @PostMapping
    public ResponseEntity<SchemaId> registerSchema(
            @PathVariable String subject,
            @RequestBody SchemaEntity request,
            @RequestParam(defaultValue = "false") boolean normalize
    ) {
        SubjectName subjectName = SubjectName.of(subject);

        // Normalize schema if requested
        String schemaText = request.getSchemaText();
        SchemaType type = request.getSchemaType() != null ? request.getSchemaType() : SchemaType.AVRO;

        if (normalize) {
            schemaText = normalizationService.normalize(schemaText, type);
        }

        // Validate references
        List<SchemaReference> references = request.getReferences() != null ? request.getReferences() : List.of();

        // Validate schema structure (skip if has references - can't parse in isolation)
        if (references.isEmpty()) {
            schemaValidationService.validate(schemaText, type);
        }
        List<String> referenceErrors = referenceValidationService.validateReferences(references);
        if (!referenceErrors.isEmpty()) {
            throw new InvalidReferenceException(referenceErrors);
        }

        // Create schema entity
        SchemaEntity schema = SchemaEntity.builder()
                .schemaText(schemaText)
                .schemaType(type)
                .references(references)
                .metadata(request.getMetadata())
                .ruleSet(request.getRuleSet())
                .createdAt(Instant.now())
                .build();

        // Register version (with optional explicit ID for IMPORT mode)
        Integer explicitId = request.getId() != null ? request.getId().getValue() : null;
        SubjectVersion sv = subjectService.registerVersion(subjectName, schema, explicitId);

        return ResponseEntity.ok(sv.getSchemaId());
    }

    /**
     * GET /subjects/{subject}/versions/{version} - Get specific version.
     */
    @GetMapping("/{version}")
    public ResponseEntity<Map<String, Object>> getVersion(
            @PathVariable String subject,
            @PathVariable String version,
            @RequestParam(defaultValue = "false") boolean deleted
    ) {
        SubjectName subjectName = SubjectName.of(subject);
        Version versionObj = Version.of(version);

        SubjectVersion sv = subjectService.getVersion(subjectName, versionObj, deleted);
        SchemaEntity schema = schemaService.getById(sv.getSchemaId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("subject", sv.getSubject().getValue());
        response.put("version", sv.getVersion().getValue());
        response.put("id", sv.getSchemaId().getValue());
        response.put("schemaType", schema.getSchemaType().name());
        response.put("schema", schema.getSchemaText());
        if (schema.getReferences() != null && !schema.getReferences().isEmpty()) {
            response.put("references", schema.getReferences());
        }
        if (schema.getMetadata() != null) {
            response.put("metadata", schema.getMetadata());
        }
        if (schema.getRuleSet() != null) {
            response.put("ruleSet", schema.getRuleSet());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * GET /subjects/{subject}/versions/{version}/schema - Get raw schema.
     */
    @GetMapping("/{version}/schema")
    public ResponseEntity<String> getSchemaString(
            @PathVariable String subject,
            @PathVariable String version
    ) {
        SubjectName subjectName = SubjectName.of(subject);
        Version versionObj = Version.of(version);

        SubjectVersion sv = subjectService.getVersion(subjectName, versionObj, false);
        SchemaEntity schema = schemaService.getById(sv.getSchemaId());

        return ResponseEntity.ok(schema.getSchemaText());
    }

    /**
     * GET /subjects/{subject}/versions/{version}/referencedby - Get schema IDs that reference this version.
     */
    @GetMapping("/{version}/referencedby")
    public ResponseEntity<List<Integer>> getReferencedBy(
            @PathVariable String subject,
            @PathVariable String version
    ) {
        SubjectName subjectName = SubjectName.of(subject);
        Version versionObj = Version.of(version);

        // Verify the subject-version exists
        subjectService.getVersion(subjectName, versionObj, false);

        // Find all schema IDs that reference this subject-version
        List<Integer> referencingSchemaIds = schemaService.getSchemaIdsReferencingSubjectVersion(subjectName, versionObj);

        return ResponseEntity.ok(referencingSchemaIds);
    }

    /**
     * DELETE /subjects/{subject}/versions/{version} - Delete version.
     */
    @DeleteMapping("/{version}")
    public ResponseEntity<Integer> deleteVersion(
            @PathVariable String subject,
            @PathVariable String version,
            @RequestParam(defaultValue = "false") boolean permanent
    ) {
        SubjectName subjectName = SubjectName.of(subject);
        Version versionObj = Version.of(version);

        int deletedVersion = subjectService.deleteVersion(subjectName, versionObj, permanent);
        return ResponseEntity.ok(deletedVersion);
    }
}
