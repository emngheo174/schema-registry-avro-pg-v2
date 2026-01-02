package com.example.schemaregistry.repository;

import com.example.schemaregistry.model.Schema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemaRepository extends JpaRepository<Schema, Long> {

    List<Schema> findBySubject(String subject);

    Optional<Schema> findBySubjectAndVersion(String subject, Integer version);

    @Query("SELECT s FROM Schema s WHERE s.subject = ?1 ORDER BY s.version DESC LIMIT 1")
    Optional<Schema> findLatestBySubject(String subject);

    @Query("SELECT s FROM Schema s WHERE s.subject = ?1 ORDER BY s.version DESC LIMIT 1 FOR UPDATE")
    Optional<Schema> findLatestBySubjectForUpdate(String subject);

    @Query("SELECT DISTINCT s.subject FROM Schema s")
    List<String> findAllSubjects();
}