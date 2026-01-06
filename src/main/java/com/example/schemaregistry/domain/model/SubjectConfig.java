package com.example.schemaregistry.domain.model;

import com.example.schemaregistry.domain.value.CompatibilityLevel;
import com.example.schemaregistry.domain.value.SubjectName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubjectConfig {

    private Long id;
    private SubjectName subject;
    private CompatibilityLevel compatibilityLevel = CompatibilityLevel.BACKWARD;
    private LocalDateTime updatedAt;