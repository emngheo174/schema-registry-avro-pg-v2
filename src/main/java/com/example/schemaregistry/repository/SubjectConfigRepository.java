package com.example.schemaregistry.repository;

import com.example.schemaregistry.model.SubjectConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubjectConfigRepository extends JpaRepository<SubjectConfig, Long> {

    Optional<SubjectConfig> findBySubject(String subject);
}