package com.example.schemaregistry.model;

import jakarta.persistence.*;

@Entity
@Table(name = "subject_configs")
public class SubjectConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String subject;

    @Column(name = "compatibility_level")
    private String compatibilityLevel = "BACKWARD";

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getCompatibilityLevel() { return compatibilityLevel; }
    public void setCompatibilityLevel(String compatibilityLevel) { this.compatibilityLevel = compatibilityLevel; }
}