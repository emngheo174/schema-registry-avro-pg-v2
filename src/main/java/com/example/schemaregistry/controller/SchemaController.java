package com.example.schemaregistry.controller;

import com.example.schemaregistry.model.Schema;
import com.example.schemaregistry.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/subjects")
public class SchemaController {

    @Autowired
    private SchemaService schemaService;

    @PostMapping("/{subject}")
    public ResponseEntity<Schema> registerSchema(@PathVariable String subject, @RequestBody String schemaText) {
        try {
            Schema schema = schemaService.registerSchema(subject, schemaText);
            return ResponseEntity.ok(schema);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
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
}