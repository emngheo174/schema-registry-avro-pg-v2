package com.example.schemaregistry.service;

import com.example.schemaregistry.model.Schema;
import com.example.schemaregistry.repository.SchemaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SchemaServiceTest {

    @Mock
    private SchemaRepository schemaRepository;

    @InjectMocks
    private SchemaService schemaService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterSchema() {
        String subject = "test-subject";
        String schemaText = "{\"type\": \"record\", \"name\": \"Test\", \"fields\": [{\"name\": \"f1\", \"type\": \"string\"}]}";

        when(schemaRepository.findBySubject(subject)).thenReturn(Arrays.asList());
        when(schemaRepository.save(any(Schema.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Schema result = schemaService.registerSchema(subject, schemaText);

        assertNotNull(result);
        assertEquals(subject, result.getSubject());
        assertEquals(1, result.getVersion());
        assertEquals(schemaText, result.getSchemaText());
        verify(schemaRepository).save(any(Schema.class));
    }

    @Test
    void testRegisterInvalidSchema() {
        String subject = "test-subject";
        String invalidSchema = "invalid json";

        assertThrows(IllegalArgumentException.class, () -> schemaService.registerSchema(subject, invalidSchema));
    }
}