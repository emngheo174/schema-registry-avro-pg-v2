package io.confluent.schemaregistry.pg.infrastructure.persistence;

import io.confluent.schemaregistry.pg.domain.model.SchemaReference;
import io.confluent.schemaregistry.pg.domain.value.SchemaId;
import io.confluent.schemaregistry.pg.domain.value.SubjectName;
import io.confluent.schemaregistry.pg.domain.value.Version;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class SchemaReferenceRepository {

    private final JdbcTemplate jdbc;

    public SchemaReferenceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<SchemaReference> rowMapper = (rs, rowNum) -> SchemaReference.builder()
            .name(rs.getString("name"))
            .subject(SubjectName.of(rs.getString("subject")))
            .version(Version.of(rs.getInt("version")))
            .build();

    public List<SchemaReference> findBySchemaId(SchemaId schemaId) {
        String sql = "SELECT * FROM schema_references WHERE schema_id = ? ORDER BY name";
        return jdbc.query(sql, rowMapper, schemaId.getValue());
    }

    public void saveAll(SchemaId schemaId, List<SchemaReference> references) {
        if (references == null || references.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO schema_references (schema_id, name, subject, version) VALUES (?, ?, ?, ?)";

        jdbc.batchUpdate(sql, references, references.size(), (ps, reference) -> {
            ps.setInt(1, schemaId.getValue());
            ps.setString(2, reference.getName());
            ps.setString(3, reference.getSubject().getValue());
            ps.setInt(4, reference.getVersion().getValue());
        });
    }

    public List<Integer> findSchemaIdsReferencingSubjectVersion(SubjectName subject, Version version) {
        String sql = "SELECT DISTINCT schema_id FROM schema_references WHERE subject = ? AND version = ? ORDER BY schema_id";
        return jdbc.queryForList(sql, Integer.class, subject.getValue(), version.getValue());
    }
}
