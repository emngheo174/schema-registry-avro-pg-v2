package com.example.schemaregistry.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "schemas")
public class Schema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String subject;

    @NotNull
    private Integer version;

    @NotBlank
    @Column(name = "schema_text")
    private String schemaText;

    @NotBlank
    @Column(name = "schema_type")
    private String schemaType = "AVRO";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public String getSchemaText() { return schemaText; }
    public void setSchemaText(String schemaText) { this.schemaText = schemaText; }

    public String getSchemaType() { return schemaType; }
    public void setSchemaType(String schemaType) { this.schemaType = schemaType; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}