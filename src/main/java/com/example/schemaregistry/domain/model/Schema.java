package com.example.schemaregistry.domain.model;

import com.example.schemaregistry.domain.value.Md5Hash;
import com.example.schemaregistry.domain.value.SchemaId;
import com.example.schemaregistry.domain.value.SchemaType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schema {

    private SchemaId id;
    private String subject;
    private Integer version;
    private String schemaText;
    private SchemaType schemaType = SchemaType.AVRO;
    private Md5Hash md5Hash;
    private LocalDateTime createdAt;