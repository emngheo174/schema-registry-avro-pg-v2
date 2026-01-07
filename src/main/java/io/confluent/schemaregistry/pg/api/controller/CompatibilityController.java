package io.confluent.schemaregistry.pg.api.controller;

import io.confluent.schemaregistry.pg.domain.model.SchemaEntity;
import io.confluent.schemaregistry.pg.domain.model.SubjectVersion;
import io.confluent.schemaregistry.pg.domain.service.CompatibilityCheckService;
import io.confluent.schemaregistry.pg.domain.service.SchemaService;
import io.confluent.schemaregistry.pg.domain.service.SubjectService;
import io.confluent.schemaregistry.pg.domain.value.CompatibilityLevel;
import io.confluent.schemaregistry.pg.domain.value.SubjectName;
import io.confluent.schemaregistry.pg.domain.value.Version;
import io.confluent.schemaregistry.pg.infrastructure.persistence.ConfigRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for /compatibility endpoints.
 */
@RestController
@RequestMapping("/compatibility")
public class CompatibilityController {

    private final SubjectService subjectService;
    private final SchemaService schemaService;
    private final ConfigRepository configRepository;
    private final CompatibilityCheckService compatibilityCheckService;

    public CompatibilityController(SubjectService subjectService,
                                    SchemaService schemaService,
                                    ConfigRepository configRepository,
                                    CompatibilityCheckService compatibilityCheckService) {
        this.subjectService = subjectService;
        this.schemaService = schemaService;
        this.configRepository = configRepository;
        this.compatibilityCheckService = compatibilityCheckService;
    }

    /**
     * POST /compatibility/subjects/{subject}/versions/{version} - Test compatibility.
     */
    @PostMapping("/subjects/{subject}/versions/{version}")
    public ResponseEntity<Map<String, Boolean>> testCompatibility(
            @PathVariable String subject,
            @PathVariable String version,
            @RequestBody SchemaEntity request
    ) {
        SubjectName subjectName = SubjectName.of(subject);
        Version versionObj = Version.of(version);

        // Get the version to test against
        SubjectVersion targetVersion = subjectService.getVersion(subjectName, versionObj, false);

        // Get all schemas from this version backwards
        List<Integer> versions = subjectService.listVersions(subjectName, false);
        List<String> existingSchemas = new ArrayList<>();

        for (int i = versions.size() - 1; i >= 0; i--) {
            int v = versions.get(i);
            if (v > targetVersion.getVersion().getValue()) {
                continue; // Skip versions newer than target
            }
            SubjectVersion sv = subjectService.getVersion(subjectName, Version.of(v), false);
            SchemaEntity schema = schemaService.getById(sv.getSchemaId());
            existingSchemas.add(schema.getSchemaText());

            if (v == targetVersion.getVersion().getValue()) {
                break; // Stop at target version
            }
        }

        // Get compatibility level
        CompatibilityLevel compatibilityLevel = configRepository.getEffectiveCompatibility(subjectName);

        // Check compatibility
        List<String> errors = compatibilityCheckService.checkCompatibility(
                request.getSchemaText(),
                existingSchemas,
                compatibilityLevel
        );

        Map<String, Boolean> response = new LinkedHashMap<>();
        response.put("is_compatible", errors.isEmpty());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /compatibility/subjects/{subject}/versions - Test compatibility against latest.
     */
    @PostMapping("/subjects/{subject}/versions")
    public ResponseEntity<Map<String, Boolean>> testCompatibilityLatest(
            @PathVariable String subject,
            @RequestBody SchemaEntity request
    ) {
        return testCompatibility(subject, "latest", request);
    }
}
