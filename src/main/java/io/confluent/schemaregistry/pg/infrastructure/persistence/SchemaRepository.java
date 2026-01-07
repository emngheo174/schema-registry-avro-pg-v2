package io.confluent.schemaregistry.pg.infrastructure.persistence;

import io.confluent.schemaregistry.pg.domain.model.SchemaEntity;
import io.confluent.schemaregistry.pg.domain.value.Md5Hash;
import io.confluent.schemaregistry.pg.domain.value.SchemaId;
import io.confluent.schemaregistry.pg.domain.value.SchemaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Repository
public class SchemaRepository {

    private final JdbcTemplate jdbc;

    public SchemaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<SchemaEntity> rowMapper = (rs, rowNum) -> SchemaEntity.builder()
            .id(SchemaId.of(rs.getInt("id")))
            .schemaText(rs.getString("schema_text"))
            .schemaType(SchemaType.from(rs.getString("schema_type")))
            .md5Hash(Md5Hash.of(rs.getString("md5_hash")))
            .createdAt(rs.getTimestamp("created_at").toInstant())
            .build();

    public Optional<SchemaEntity> findById(SchemaId schemaId) {
        String sql = "SELECT * FROM schemas WHERE id = ?";
        List<SchemaEntity> results = jdbc.query(sql, rowMapper, schemaId.getValue());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public Optional<SchemaEntity> findByHash(Md5Hash hash) {
        String sql = "SELECT * FROM schemas WHERE md5_hash = ?";
        List<SchemaEntity> results = jdbc.query(sql, rowMapper, hash.getValue());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public SchemaEntity save(SchemaEntity schema) {
        String sql = "INSERT INTO schemas (schema_text, schema_type, md5_hash, created_at) " +
                     "VALUES (?, ?, ?, ?) RETURNING id";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, schema.getSchemaText());
            ps.setString(2, schema.getSchemaType().name());
            ps.setString(3, schema.getMd5Hash().getValue());
            ps.setTimestamp(4, Timestamp.from(schema.getCreatedAt()));
            return ps;
        }, keyHolder);

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Failed to get generated key for schema");
        }

        return schema.withId(SchemaId.of(key.intValue()));
    }

    public SchemaEntity saveWithId(SchemaEntity schema, int explicitId) {
        String sql = "INSERT INTO schemas (id, schema_text, schema_type, md5_hash, created_at) " +
                     "VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT (md5_hash) DO UPDATE SET id = EXCLUDED.id RETURNING id";

        Integer savedId = jdbc.queryForObject(sql, Integer.class,
                explicitId,
                schema.getSchemaText(),
                schema.getSchemaType().name(),
                schema.getMd5Hash().getValue(),
                Timestamp.from(schema.getCreatedAt())
        );

        return schema.withId(SchemaId.of(savedId));
    }

    public List<SchemaId> findAllIds(int limit, int offset) {
        String sql = "SELECT id FROM schemas ORDER BY id LIMIT ? OFFSET ?";
        return jdbc.query(sql, (rs, rowNum) -> SchemaId.of(rs.getInt("id")), limit, offset);
    }
}
