package io.confluent.schemaregistry.pg.api.controller;

import io.confluent.schemaregistry.pg.domain.model.SchemaEntity;
import io.confluent.schemaregistry.pg.domain.model.SubjectVersion;
import io.confluent.schemaregistry.pg.domain.service.SchemaService;
import io.confluent.schemaregistry.pg.domain.service.SubjectService;
import io.confluent.schemaregistry.pg.domain.value.Md5Hash;
import io.confluent.schemaregistry.pg.domain.value.SubjectName;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for /subjects endpoints.
 */
@RestController
@RequestMapping("/subjects")
public class SubjectsController {

    private final SubjectService subjectService;
    private final SchemaService schemaService;

    public SubjectsController(SubjectService subjectService, SchemaService schemaService) {
        this.subjectService = subjectService;
        this.schemaService = schemaService;
    }

    /**
     * GET /subjects - List all subjects.
     */
    @GetMapping
    public ResponseEntity<List<String>> listSubjects(
            @RequestParam(required = false) String subjectPrefix,
            @RequestParam(defaultValue = "false") boolean deleted,
            @RequestParam(defaultValue = "false") boolean deletedOnly
    ) {
        List<String> subjects = subjectService.listSubjects(subjectPrefix, deleted, deletedOnly);
        return ResponseEntity.ok(subjects);
    }

    /**
     * POST /subjects/{subject} - Lookup schema under subject.
     */
    @PostMapping("/{subject}")
    public ResponseEntity<Map<String, Object>> lookupSchema(
            @PathVariable String subject,
            @RequestBody SchemaEntity request,
            @RequestParam(defaultValue = "false") boolean normalize,
            @RequestParam(defaultValue = "false") boolean deleted
    ) {
        SubjectName subjectName = SubjectName.of(subject);
        Md5Hash schemaHash = Md5Hash.compute(request.getSchemaText());

        SubjectVersion sv = subjectService.lookupSchema(subjectName, schemaHash);
        SchemaEntity schema = schemaService.getById(sv.getSchemaId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("subject", sv.getSubject().getValue());
        response.put("id", sv.getSchemaId().getValue());
        response.put("version", sv.getVersion().getValue());
        response.put("schema", schema.getSchemaText());

        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /subjects/{subject} - Delete subject.
     */
    @DeleteMapping("/{subject}")
    public ResponseEntity<List<Integer>> deleteSubject(
            @PathVariable String subject,
            @RequestParam(defaultValue = "false") boolean permanent
    ) {
        SubjectName subjectName = SubjectName.of(subject);
        List<Integer> deletedVersions = subjectService.deleteSubject(subjectName, permanent);
        return ResponseEntity.ok(deletedVersions);
    }
}
