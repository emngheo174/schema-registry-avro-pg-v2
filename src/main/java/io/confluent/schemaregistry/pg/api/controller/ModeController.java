package io.confluent.schemaregistry.pg.api.controller;

import io.confluent.schemaregistry.pg.domain.model.GlobalConfig;
import io.confluent.schemaregistry.pg.domain.model.SubjectConfig;
import io.confluent.schemaregistry.pg.domain.value.Mode;
import io.confluent.schemaregistry.pg.domain.value.SubjectName;
import io.confluent.schemaregistry.pg.infrastructure.persistence.ConfigRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for /mode endpoints.
 */
@RestController
@RequestMapping("/mode")
public class ModeController {

    private final ConfigRepository configRepository;

    public ModeController(ConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    /**
     * GET /mode - Get global mode.
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> getGlobalMode() {
        GlobalConfig config = configRepository.getGlobalConfig();
        Map<String, String> response = new LinkedHashMap<>();
        response.put("mode", config.getMode().name());
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /mode - Update global mode.
     */
    @PutMapping
    public ResponseEntity<Map<String, String>> updateGlobalMode(@RequestBody Map<String, String> request) {
        String modeStr = request.get("mode");
        Mode mode = Mode.from(modeStr);

        GlobalConfig currentConfig = configRepository.getGlobalConfig();
        GlobalConfig updatedConfig = currentConfig.withMode(mode);
        configRepository.updateGlobalConfig(updatedConfig);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("mode", mode.name());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /mode/{subject} - Get subject-level mode.
     */
    @GetMapping("/{subject}")
    public ResponseEntity<Map<String, String>> getSubjectMode(@PathVariable String subject) {
        SubjectName subjectName = SubjectName.of(subject);
        Mode mode = configRepository.getEffectiveMode(subjectName);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("mode", mode.name());
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /mode/{subject} - Update subject-level mode.
     */
    @PutMapping("/{subject}")
    public ResponseEntity<Map<String, String>> updateSubjectMode(
            @PathVariable String subject,
            @RequestBody Map<String, String> request
    ) {
        SubjectName subjectName = SubjectName.of(subject);
        String modeStr = request.get("mode");
        Mode mode = Mode.from(modeStr);

        SubjectConfig config = SubjectConfig.builder()
                .subject(subjectName)
                .mode(mode)
                .build();

        configRepository.saveSubjectConfig(config);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("mode", mode.name());
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /mode/{subject} - Delete subject-level mode (revert to global).
     */
    @DeleteMapping("/{subject}")
    public ResponseEntity<Map<String, String>> deleteSubjectMode(@PathVariable String subject) {
        SubjectName subjectName = SubjectName.of(subject);
        configRepository.deleteSubjectConfig(subjectName);

        Mode mode = configRepository.getEffectiveMode(subjectName);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("mode", mode.name());
        return ResponseEntity.ok(response);
    }
}
