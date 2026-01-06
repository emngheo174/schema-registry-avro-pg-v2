package com.example.schemaregistry.domain.service;

import com.example.schemaregistry.domain.model.SchemaEntity;
import com.example.schemaregistry.domain.model.SubjectVersion;
import com.example.schemaregistry.domain.value.*;
import com.example.schemaregistry.infrastructure.persistence.SchemaRepository;
import com.example.schemaregistry.infrastructure.persistence.SubjectConfigRepository;
import com.example.schemaregistry.infrastructure.persistence.SubjectVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SchemaServiceTest {

    @Mock
    private SchemaRepository schemaRepository;

    @Mock
    private SubjectVersionRepository subjectVersionRepository;

    @Mock
    private SubjectConfigRepository subjectConfigRepository;

    private SchemaService schemaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        schemaService = new SchemaService(schemaRepository, subjectVersionRepository, subjectConfigRepository);
    }

    @Test
    void testRegisterSchema() {
        // Given
        String subject = "test-subject";
        String schemaText = "{\"type\": \"record\", \"name\": \"Test\", \"fields\": [{\"name\": \"id\", \"type\": \"string\"}]}";

        SchemaEntity schemaEntity = SchemaEntity.builder()
                .schemaText(schemaText)
                .schemaType(SchemaType.AVRO)
                .md5Hash(Md5Hash.compute(schemaText))
                .build();

        when(subjectVersionRepository.countBySubjectAndDeletedFalse(any(SubjectName.class))).thenReturn(0);
        when(schemaRepository.findByMd5Hash(any(Md5Hash.class))).thenReturn(Optional.empty());
        when(schemaRepository.save(any(SchemaEntity.class))).thenReturn(schemaEntity.toBuilder().id(SchemaId.of(1)).build());
        when(subjectVersionRepository.save(any(SubjectVersion.class))).thenReturn(new SubjectVersion());

        // When
        SchemaId result = schemaService.registerSchema(subject, schemaText);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getValue());
    }
}