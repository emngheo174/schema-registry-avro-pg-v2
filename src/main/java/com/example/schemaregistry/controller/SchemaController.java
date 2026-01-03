package com.example.schemaregistry.controller;

import com.example.schemaregistry.model.ErrorResponse;
import com.example.schemaregistry.model.Schema;
import com.example.schemaregistry.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/subjects")
public class SchemaController {

    @Autowired
    private SchemaService schemaService;

    @Value("${schema.registry.compatibility.level:BACKWARD}")
    private String compatibilityLevel;

    @PostMapping("/{subject}")
    public ResponseEntity<?> registerSchema(@PathVariable String subject, @RequestBody String schemaText) {
        try {
            Schema schema = schemaService.registerSchema(subject, schemaText);
            return ResponseEntity.ok(schema);
        } catch (IllegalArgumentException e) {
            ErrorResponse error = new ErrorResponse(42201, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse(50001, "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{subject}/versions/{version}")
    public ResponseEntity<Schema> getSchema(@PathVariable String subject, @PathVariable Integer version) {
        Optional<Schema> schema = schemaService.getSchema(subject, version);
        return schema.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{subject}/versions")
    public ResponseEntity<List<Schema>> getAllVersions(@PathVariable String subject) {
        List<Schema> schemas = schemaService.getAllVersions(subject);
        return ResponseEntity.ok(schemas);
    }

    @GetMapping("/{subject}")
    public ResponseEntity<Schema> getLatestSchema(@PathVariable String subject) {
        Optional<Schema> schema = schemaService.getLatestSchema(subject);
        return schema.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, String>> getConfig() {
        return ResponseEntity.ok(Map.of("compatibility", compatibilityLevel));
    }

    @PutMapping("/config")
    public ResponseEntity<Void> updateGlobalConfig(@RequestBody Map<String, String> config) {
        // For simplicity, global config is read-only in this implementation
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{subject}/compatibility")
    public ResponseEntity<?> checkCompatibility(@PathVariable String subject, @RequestBody String schemaText) {
        try {
            boolean compatible = schemaService.isCompatible(subject, schemaText, compatibilityLevel);
            return ResponseEntity.ok(Map.of("is_compatible", compatible));
        } catch (IllegalArgumentException e) {
            ErrorResponse error = new ErrorResponse(42201, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse(50001, "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    @PutMapping("/{subject}/config")
    public ResponseEntity<?> updateConfig(@PathVariable String subject, @RequestBody Map<String, String> config) {
        try {
            String level = config.get("compatibility");
            if (level == null) {
                ErrorResponse error = new ErrorResponse(42202, "Missing compatibility level in request body");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            // Validate compatibility level
            String levelUpper = level.toUpperCase();
            if (!levelUpper.equals("NONE") && !levelUpper.equals("BACKWARD") &&
                !levelUpper.equals("FORWARD") && !levelUpper.equals("FULL")) {
                ErrorResponse error = new ErrorResponse(42203, "Invalid compatibility level: " + level + ". Valid values: NONE, BACKWARD, FORWARD, FULL");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            schemaService.setCompatibilityLevel(subject, level);
            return ResponseEntity.ok(Map.of("compatibility", level));
        } catch (Exception e) {
            ErrorResponse error = new ErrorResponse(50001, "Internal server error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/{subject}/config")
    public ResponseEntity<Map<String, String>> getSubjectConfig(@PathVariable String subject) {
        String level = schemaService.getCompatibilityLevelForSubject(subject);
        return ResponseEntity.ok(Map.of("compatibility", level));
    }

    @GetMapping("")
    public ResponseEntity<List<String>> getAllSubjects() {
        List<String> subjects = schemaService.getAllSubjects();
        return ResponseEntity.ok(subjects);
    }

    @DeleteMapping("/{subject}/versions/{version}")
    public ResponseEntity<Void> deleteSchema(@PathVariable String subject, @PathVariable Integer version) {
        schemaService.deleteSchema(subject, version);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{subject}")
    public ResponseEntity<Void> deleteSubject(@PathVariable String subject) {
        schemaService.deleteSubject(subject);
        return ResponseEntity.ok().build();
    }}