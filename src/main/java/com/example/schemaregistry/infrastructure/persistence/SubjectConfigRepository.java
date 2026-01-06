package com.example.schemaregistry.infrastructure.persistence;

import com.example.schemaregistry.domain.model.SubjectConfig;
import com.example.schemaregistry.domain.value.SubjectName;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubjectConfigRepository extends CrudRepository<SubjectConfig, Long> {

    Optional<SubjectConfig> findBySubject(SubjectName subject);
}