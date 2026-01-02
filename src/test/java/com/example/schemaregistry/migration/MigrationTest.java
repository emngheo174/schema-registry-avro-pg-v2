package com.example.schemaregistry.migration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class MigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void testMigrationApplied() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'schemas'", Integer.class);
        assertTrue(count > 0, "Schemas table should exist after migration");
    }
}