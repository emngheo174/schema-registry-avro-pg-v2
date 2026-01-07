package io.confluent.schemaregistry.pg.infrastructure.persistence;

import io.confluent.schemaregistry.pg.domain.model.SubjectVersion;
import io.confluent.schemaregistry.pg.domain.value.Md5Hash;
import io.confluent.schemaregistry.pg.domain.value.SchemaId;
import io.confluent.schemaregistry.pg.domain.value.SubjectName;
import io.confluent.schemaregistry.pg.domain.value.Version;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class SubjectVersionRepository {

    private final JdbcTemplate jdbc;

    public SubjectVersionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<SubjectVersion> rowMapper = (rs, rowNum) -> SubjectVersion.builder()
            .id(rs.getLong("id"))
            .subject(SubjectName.of(rs.getString("subject")))
            .version(Version.of(rs.getInt("version")))
            .schemaId(SchemaId.of(rs.getInt("schema_id")))
            .deleted(rs.getBoolean("deleted"))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .build();

    public List<String> findAllSubjects(boolean includeDeleted) {
        String sql = includeDeleted
                ? "SELECT DISTINCT subject FROM subject_versions ORDER BY subject"
                : "SELECT DISTINCT subject FROM subject_versions WHERE deleted = false ORDER BY subject";
        return jdbc.queryForList(sql, String.class);
    }

    public List<String> findSubjectsWithPrefix(String prefix, boolean includeDeleted) {
        String sql = includeDeleted
                ? "SELECT DISTINCT subject FROM subject_versions WHERE subject LIKE ? ORDER BY subject"
                : "SELECT DISTINCT subject FROM subject_versions WHERE subject LIKE ? AND deleted = false ORDER BY subject";
        return jdbc.queryForList(sql, String.class, prefix + "%");
    }

    public List<String> findDeletedSubjects() {
        String sql = "SELECT DISTINCT subject FROM subject_versions WHERE deleted = true ORDER BY subject";
        return jdbc.queryForList(sql, String.class);
    }

    public List<Integer> findVersionsBySubject(SubjectName subject, boolean includeDeleted) {
        String sql = includeDeleted
                ? "SELECT version FROM subject_versions WHERE subject = ? ORDER BY version"
                : "SELECT version FROM subject_versions WHERE subject = ? AND deleted = false ORDER BY version";
        return jdbc.queryForList(sql, Integer.class, subject.getValue());
    }

    public Optional<SubjectVersion> findBySubjectAndVersion(SubjectName subject, Version version, boolean includeDeleted) {
        if (version.isLatest()) {
            return findLatestVersion(subject, includeDeleted);
        }

        String sql = includeDeleted
                ? "SELECT * FROM subject_versions WHERE subject = ? AND version = ?"
                : "SELECT * FROM subject_versions WHERE subject = ? AND version = ? AND deleted = false";
        List<SubjectVersion> results = jdbc.query(sql, rowMapper, subject.getValue(), version.getValue());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<SubjectVersion> findLatestVersion(SubjectName subject, boolean includeDeleted) {
        String sql = includeDeleted
                ? "SELECT * FROM subject_versions WHERE subject = ? ORDER BY version DESC LIMIT 1"
                : "SELECT * FROM subject_versions WHERE subject = ? AND deleted = false ORDER BY version DESC LIMIT 1";
        List<SubjectVersion> results = jdbc.query(sql, rowMapper, subject.getValue());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<SubjectVersion> findBySubjectAndHash(SubjectName subject, Md5Hash hash) {
        String sql = """
                SELECT sv.* FROM subject_versions sv
                JOIN schemas s ON sv.schema_id = s.id
                WHERE sv.subject = ? AND s.md5_hash = ? AND sv.deleted = false
                ORDER BY sv.version DESC LIMIT 1
                """;
        List<SubjectVersion> results = jdbc.query(sql, rowMapper, subject.getValue(), hash.getValue());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public SubjectVersion save(SubjectVersion subjectVersion) {
        String sql = "INSERT INTO subject_versions (subject, version, schema_id, deleted, created_at) " +
                     "VALUES (?, ?, ?, ?, ?) RETURNING id";

        Long id = jdbc.queryForObject(sql, Long.class,
                subjectVersion.getSubject().getValue(),
                subjectVersion.getVersion().getValue(),
                subjectVersion.getSchemaId().getValue(),
                subjectVersion.isDeleted(),
                Timestamp.from(subjectVersion.getCreatedAt())
        );

        return subjectVersion.withId(id);
    }

    public int getNextVersion(SubjectName subject) {
        String sql = "SELECT COALESCE(MAX(version), 0) + 1 FROM subject_versions WHERE subject = ?";
        Integer nextVersion = jdbc.queryForObject(sql, Integer.class, subject.getValue());
        return nextVersion != null ? nextVersion : 1;
    }

    public void softDelete(SubjectName subject, Version version) {
        String sql = "UPDATE subject_versions SET deleted = true WHERE subject = ? AND version = ?";
        jdbc.update(sql, subject.getValue(), version.getValue());
    }

    public void softDeleteAllVersions(SubjectName subject) {
        String sql = "UPDATE subject_versions SET deleted = true WHERE subject = ?";
        jdbc.update(sql, subject.getValue());
    }

    public void hardDelete(SubjectName subject, Version version) {
        String sql = "DELETE FROM subject_versions WHERE subject = ? AND version = ?";
        jdbc.update(sql, subject.getValue(), version.getValue());
    }

    public void hardDeleteAllVersions(SubjectName subject) {
        String sql = "DELETE FROM subject_versions WHERE subject = ?";
        jdbc.update(sql, subject.getValue());
    }

    public boolean existsBySubject(SubjectName subject) {
        String sql = "SELECT COUNT(*) > 0 FROM subject_versions WHERE subject = ? AND deleted = false";
        Boolean exists = jdbc.queryForObject(sql, Boolean.class, subject.getValue());
        return Boolean.TRUE.equals(exists);
    }

    public boolean existsBySubjectAndVersion(SubjectName subject, Version version) {
        String sql = "SELECT COUNT(*) > 0 FROM subject_versions WHERE subject = ? AND version = ?";
        Boolean exists = jdbc.queryForObject(sql, Boolean.class, subject.getValue(), version.getValue());
        return Boolean.TRUE.equals(exists);
    }
}
