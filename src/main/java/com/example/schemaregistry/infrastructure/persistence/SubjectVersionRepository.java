package com.example.schemaregistry.infrastructure.persistence;

import com.example.schemaregistry.domain.model.SubjectVersion;
import com.example.schemaregistry.domain.value.SubjectName;
import com.example.schemaregistry.domain.value.Version;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectVersionRepository extends CrudRepository<SubjectVersion, Long> {

    List<SubjectVersion> findBySubject(SubjectName subject);

    List<SubjectVersion> findBySubjectAndDeletedFalse(SubjectName subject);

    Optional<SubjectVersion> findBySubjectAndVersion(SubjectName subject, Version version);

    Optional<SubjectVersion> findBySubjectAndVersionAndDeletedFalse(SubjectName subject, Version version);

    @Query("SELECT * FROM subject_versions WHERE subject = :subject AND deleted = false ORDER BY version DESC LIMIT 1")
    Optional<SubjectVersion> findLatestBySubject(SubjectName subject);

    @Query("SELECT DISTINCT subject FROM subject_versions WHERE deleted = false")
    List<String> findAllActiveSubjects();

    @Query("SELECT COUNT(*) FROM subject_versions WHERE subject = :subject AND deleted = false")
    int countBySubjectAndDeletedFalse(SubjectName subject);
}