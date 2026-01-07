package io.confluent.schemaregistry.pg.infrastructure.persistence;

import io.confluent.schemaregistry.pg.domain.model.GlobalConfig;
import io.confluent.schemaregistry.pg.domain.model.SubjectConfig;
import io.confluent.schemaregistry.pg.domain.value.CompatibilityLevel;
import io.confluent.schemaregistry.pg.domain.value.Mode;
import io.confluent.schemaregistry.pg.domain.value.SubjectName;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ConfigRepository {

    private final JdbcTemplate jdbc;

    public ConfigRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<GlobalConfig> globalConfigMapper = (rs, rowNum) -> GlobalConfig.builder()
            .compatibility(CompatibilityLevel.from(rs.getString("compatibility")))
            .mode(Mode.from(rs.getString("mode")))
            .compatibilityGroup(rs.getString("compatibility_group"))
            .updatedAt(rs.getTimestamp("updated_at").toInstant())
            .build();

    private final RowMapper<SubjectConfig> subjectConfigMapper = (rs, rowNum) -> {
        SubjectConfig.SubjectConfigBuilder builder = SubjectConfig.builder()
                .subject(SubjectName.of(rs.getString("subject")))
                .normalize(rs.getBoolean("normalize"))
                .updatedAt(rs.getTimestamp("updated_at").toInstant());

        String compatibility = rs.getString("compatibility");
        if (compatibility != null) {
            builder.compatibility(CompatibilityLevel.from(compatibility));
        }

        String mode = rs.getString("mode");
        if (mode != null) {
            builder.mode(Mode.from(mode));
        }

        String compatibilityGroup = rs.getString("compatibility_group");
        if (compatibilityGroup != null) {
            builder.compatibilityGroup(compatibilityGroup);
        }

        String alias = rs.getString("alias");
        if (alias != null) {
            builder.alias(alias);
        }

        return builder.build();
    };

    public GlobalConfig getGlobalConfig() {
        String sql = "SELECT * FROM global_config ORDER BY id DESC LIMIT 1";
        List<GlobalConfig> results = jdbc.query(sql, globalConfigMapper);
        return results.isEmpty()
                ? GlobalConfig.builder().build()
                : results.get(0);
    }

    public void updateGlobalConfig(GlobalConfig config) {
        String sql = "UPDATE global_config SET compatibility = ?, mode = ?, compatibility_group = ?, updated_at = CURRENT_TIMESTAMP";
        jdbc.update(sql,
                config.getCompatibility().name(),
                config.getMode().name(),
                config.getCompatibilityGroup()
        );
    }

    public Optional<SubjectConfig> findSubjectConfig(SubjectName subject) {
        String sql = "SELECT * FROM subject_config WHERE subject = ?";
        List<SubjectConfig> results = jdbc.query(sql, subjectConfigMapper, subject.getValue());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    public void saveSubjectConfig(SubjectConfig config) {
        String sql = """
                INSERT INTO subject_config (subject, compatibility, compatibility_group, mode, alias, normalize, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT (subject) DO UPDATE SET
                    compatibility = EXCLUDED.compatibility,
                    compatibility_group = EXCLUDED.compatibility_group,
                    mode = EXCLUDED.mode,
                    alias = EXCLUDED.alias,
                    normalize = EXCLUDED.normalize,
                    updated_at = CURRENT_TIMESTAMP
                """;

        jdbc.update(sql,
                config.getSubject().getValue(),
                config.getCompatibility() != null ? config.getCompatibility().name() : null,
                config.getCompatibilityGroup(),
                config.getMode() != null ? config.getMode().name() : null,
                config.getAlias(),
                config.isNormalize()
        );
    }

    public void deleteSubjectConfig(SubjectName subject) {
        String sql = "DELETE FROM subject_config WHERE subject = ?";
        jdbc.update(sql, subject.getValue());
    }

    public CompatibilityLevel getEffectiveCompatibility(SubjectName subject) {
        Optional<SubjectConfig> subjectConfig = findSubjectConfig(subject);
        if (subjectConfig.isPresent() && subjectConfig.get().getCompatibility() != null) {
            return subjectConfig.get().getCompatibility();
        }
        return getGlobalConfig().getCompatibility();
    }

    public Mode getEffectiveMode(SubjectName subject) {
        Optional<SubjectConfig> subjectConfig = findSubjectConfig(subject);
        if (subjectConfig.isPresent() && subjectConfig.get().getMode() != null) {
            return subjectConfig.get().getMode();
        }
        return getGlobalConfig().getMode();
    }
}
