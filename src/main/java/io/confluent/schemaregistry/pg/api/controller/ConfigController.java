package io.confluent.schemaregistry.pg.api.controller;

import io.confluent.schemaregistry.pg.domain.model.GlobalConfig;
import io.confluent.schemaregistry.pg.domain.model.SubjectConfig;
import io.confluent.schemaregistry.pg.domain.value.CompatibilityLevel;
import io.confluent.schemaregistry.pg.domain.value.SubjectName;
import io.confluent.schemaregistry.pg.infrastructure.persistence.ConfigRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for /config endpoints.
 */
@RestController
@RequestMapping("/config")
public class ConfigController {

    private final ConfigRepository configRepository;

    public ConfigController(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    /**
     * GET /config - Get global compatibility level.
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> getGlobalConfig() {
        GlobalConfig config = configRepository.getGlobalConfig();
        Map<String, String> response = new LinkedHashMap<>();
        response.put("compatibilityLevel", config.getCompatibility().name());
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /config - Update global compatibility level.
     */
    @PutMapping
    public ResponseEntity<Map<String, String>> updateGlobalConfig(@RequestBody Map<String, String> request) {
        String compatibilityStr = request.get("compatibility");
        if (compatibilityStr == null) {
            compatibilityStr = request.get("compatibilityLevel");
        }

        CompatibilityLevel compatibility = CompatibilityLevel.from(compatibilityStr);

        GlobalConfig currentConfig = configRepository.getGlobalConfig();
        GlobalConfig updatedConfig = currentConfig.withCompatibility(compatibility);
        configRepository.updateGlobalConfig(updatedConfig);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("compatibility", compatibility.name());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /config/{subject} - Get subject-level compatibility.
     */
    @GetMapping("/{subject}")
    public ResponseEntity<Map<String, String>> getSubjectConfig(@PathVariable String subject) {
        SubjectName subjectName = SubjectName.of(subject);
        CompatibilityLevel compatibility = configRepository.getEffectiveCompatibility(subjectName);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("compatibilityLevel", compatibility.name());
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /config/{subject} - Update subject-level compatibility.
     */
    @PutMapping("/{subject}")
    public ResponseEntity<Map<String, String>> updateSubjectConfig(
            @PathVariable String subject,
            @RequestBody Map<String, String> request
    ) {
        SubjectName subjectName = SubjectName.of(subject);

        String compatibilityStr = request.get("compatibility");
        if (compatibilityStr == null) {
            compatibilityStr = request.get("compatibilityLevel");
        }

        CompatibilityLevel compatibility = CompatibilityLevel.from(compatibilityStr);

        SubjectConfig config = SubjectConfig.builder()
                .subject(subjectName)
                .compatibility(compatibility)
                .build();

        configRepository.saveSubjectConfig(config);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("compatibility", compatibility.name());
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /config/{subject} - Delete subject-level config (revert to global).
     */
    @DeleteMapping("/{subject}")
    public ResponseEntity<Map<String, String>> deleteSubjectConfig(@PathVariable String subject) {
        SubjectName subjectName = SubjectName.of(subject);
        configRepository.deleteSubjectConfig(subjectName);

        // Return the new effective config (global)
        CompatibilityLevel compatibility = configRepository.getEffectiveCompatibility(subjectName);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("compatibility", compatibility.name());
        return ResponseEntity.ok(response);
    }
}
