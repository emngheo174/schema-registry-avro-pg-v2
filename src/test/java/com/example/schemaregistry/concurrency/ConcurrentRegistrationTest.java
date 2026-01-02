package com.example.schemaregistry.concurrency;

import com.example.schemaregistry.model.Schema;
import com.example.schemaregistry.repository.SchemaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class ConcurrentRegistrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(org.springframework.test.context.DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private SchemaRepository schemaRepository;

    @Test
    void concurrentRegistrations_shouldAssignUniqueVersions() throws InterruptedException {
        String subject = "test-subject";
        int threads = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        Set<Integer> versions = ConcurrentHashMap.newKeySet();

        for (int i = 0; i < threads; i++) {
            int schemaNum = i;
            executor.submit(() -> {
                try {
                    // Simulate registration logic
                    String schemaText = "{\"type\": \"record\", \"name\": \"Test" + schemaNum + "\", \"fields\": [{\"name\": \"f1\", \"type\": \"string\"}]}";

                    // Parse and validate (simplified)
                    org.apache.avro.Schema.Parser parser = new org.apache.avro.Schema.Parser();
                    parser.parse(schemaText);

                    // Get next version with lock (simplified, in real service it's transactional)
                    var latest = schemaRepository.findLatestBySubjectForUpdate(subject);
                    int nextVersion = latest.map(s -> s.getVersion() + 1).orElse(1);

                    Schema schema = new Schema();
                    schema.setSubject(subject);
                    schema.setVersion(nextVersion);
                    schema.setSchemaText(schemaText);
                    schema.setSchemaType("AVRO");

                    schemaRepository.save(schema);
                    versions.add(nextVersion);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Should have 50 unique versions
        assertEquals(threads, versions.size(), "Expected " + threads + " unique versions, got " + versions.size());
    }
}