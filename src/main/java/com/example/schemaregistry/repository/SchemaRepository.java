package com.example.schemaregistry.repository;

import com.example.schemaregistry.model.Schema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemaRepository extends JpaRepository<Schema, Long> {

    List<Schema> findBySubject(String subject);

    Optional<Schema> findBySubjectAndVersion(String subject, Integer version);
}