package io.confluent.schemaregistry.pg.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller for root endpoint.
 */
@RestController
@RequestMapping("/")
public class RootController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getRoot() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("version", "1.0.0");
        response.put("implementation", "PostgreSQL-backed Schema Registry");
        response.put("schemaTypes", new String[]{"AVRO"});
        return ResponseEntity.ok(response);
    }
}
