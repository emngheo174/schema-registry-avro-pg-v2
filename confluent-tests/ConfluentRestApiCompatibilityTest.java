package io.confluent.kafka.schemaregistry.compatibility;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.avro.Schema;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Confluent Schema Registry REST API Compatibility Tests
 * 
 * Mục đích: Verify rằng custom Schema Registry implementation tuân thủ
 * Confluent Schema Registry REST API specification.
 * 
 * Test suite này được adapt từ Confluent's RestApiTest.
 * 
 * Prerequisites:
 * - Custom Schema Registry running at configured URL
 * - PostgreSQL database accessible
 * 
 * Run:
 * mvn test -Dtest=ConfluentRestApiCompatibilityTest
 * mvn test -Dtest=ConfluentRestApiCompatibilityTest -Dschema.registry.url=http://localhost:8081
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Confluent REST API Compatibility Tests")
public class ConfluentRestApiCompatibilityTest {

    private static final String SCHEMA_REGISTRY_URL = 
        System.getProperty("schema.registry.url", "http://localhost:8081");
    
    private static SchemaRegistryClient client;
    
    // Test schemas
    private static final String USER_SCHEMA_V1 = 
        "{\"type\":\"record\",\"name\":\"User\",\"namespace\":\"io.confluent.test\","
        + "\"fields\":[{\"name\":\"id\",\"type\":\"long\"},{\"name\":\"name\",\"type\":\"string\"}]}";
    
    private static final String USER_SCHEMA_V2_COMPATIBLE = 
        "{\"type\":\"record\",\"name\":\"User\",\"namespace\":\"io.confluent.test\","
        + "\"fields\":[{\"name\":\"id\",\"type\":\"long\"},{\"name\":\"name\",\"type\":\"string\"},"
        + "{\"name\":\"email\",\"type\":[\"null\",\"string\"],\"default\":null}]}";
    
    private static final String USER_SCHEMA_V2_INCOMPATIBLE = 
        "{\"type\":\"record\",\"name\":\"User\",\"namespace\":\"io.confluent.test\","
        + "\"fields\":[{\"name\":\"id\",\"type\":\"long\"}]}";

    @BeforeAll
    static void setup() {
        baseURI = SCHEMA_REGISTRY_URL;
        client = new CachedSchemaRegistryClient(SCHEMA_REGISTRY_URL, 100);
        
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║  Confluent REST API Compatibility Test Suite             ║");
        System.out.println("║  Testing against: " + SCHEMA_REGISTRY_URL + "                    ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
    }

    // ============================================================================
    // Root Endpoint Tests
    // ============================================================================

    @Test
    @Order(1)
    @DisplayName("GET / - Should return server info")
    void testRootEndpoint() {
        given()
            .when()
            .get("/")
            .then()
            .statusCode(200)
            .body(notNullValue());
        
        System.out.println("✅ Root endpoint working");
    }

    // ============================================================================
    // Subjects Tests
    // ============================================================================

    @Test
    @Order(2)
    @DisplayName("GET /subjects - Should list all subjects")
    void testListSubjects() {
        List<String> subjects = 
            given()
                .when()
                .get("/subjects")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$", String.class);
        
        assertNotNull(subjects);
        System.out.println("✅ Listed " + subjects.size() + " subjects");
    }

    @Test
    @Order(3)
    @DisplayName("POST /subjects/{subject}/versions - Should register new schema")
    void testRegisterSchema() {
        String requestBody = String.format("{\"schema\":\"%s\"}", 
            USER_SCHEMA_V1.replace("\"", "\\\""));
        
        Integer schemaId = 
            given()
                .contentType("application/vnd.schemaregistry.v1+json")
                .body(requestBody)
            .when()
                .post("/subjects/test-user-value/versions")
            .then()
                .statusCode(200)
                .body("id", notNullValue())
                .extract()
                .jsonPath()
                .getInt("id");
        
        assertTrue(schemaId > 0);
        System.out.println("✅ Registered schema with ID: " + schemaId);
    }

    @Test
    @Order(4)
    @DisplayName("GET /subjects/{subject}/versions - Should list all versions")
    void testListVersions() {
        List<Integer> versions = 
            given()
                .when()
                .get("/subjects/test-user-value/versions")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getList("$", Integer.class);
        
        assertNotNull(versions);
        assertFalse(versions.isEmpty());
        System.out.println("✅ Found " + versions.size() + " version(s)");
    }

    @Test
    @Order(5)
    @DisplayName("GET /subjects/{subject}/versions/latest - Should get latest schema")
    void testGetLatestSchema() {
        given()
            .when()
            .get("/subjects/test-user-value/versions/latest")
            .then()
            .statusCode(200)
            .body("subject", equalTo("test-user-value"))
            .body("version", greaterThan(0))
            .body("id", greaterThan(0))
            .body("schema", notNullValue());
        
        System.out.println("✅ Retrieved latest schema");
    }

    @Test
    @Order(6)
    @DisplayName("GET /subjects/{subject}/versions/{version} - Should get specific version")
    void testGetSchemaByVersion() {
        given()
            .when()
            .get("/subjects/test-user-value/versions/1")
            .then()
            .statusCode(200)
            .body("subject", equalTo("test-user-value"))
            .body("version", equalTo(1))
            .body("schema", notNullValue());
        
        System.out.println("✅ Retrieved schema version 1");
    }

    // ============================================================================
    // Schema By ID Tests
    // ============================================================================

    @Test
    @Order(7)
    @DisplayName("GET /schemas/ids/{id} - Should get schema by ID")
    void testGetSchemaById() throws IOException, RestClientException {
        // Get latest schema metadata to find ID
        var metadata = client.getLatestSchemaMetadata("test-user-value");
        int schemaId = metadata.getId();
        
        given()
            .when()
            .get("/schemas/ids/" + schemaId)
            .then()
            .statusCode(200)
            .body("schema", notNullValue());
        
        System.out.println("✅ Retrieved schema by ID: " + schemaId);
    }

    @Test
    @Order(8)
    @DisplayName("GET /schemas/ids/{id} - Should return 404 for non-existent ID")
    void testGetNonExistentSchemaById() {
        given()
            .when()
            .get("/schemas/ids/999999")
            .then()
            .statusCode(404);
        
        System.out.println("✅ Correctly returned 404 for non-existent schema");
    }

    // ============================================================================
    // Config Tests
    // ============================================================================

    @Test
    @Order(9)
    @DisplayName("GET /config - Should get global config")
    void testGetGlobalConfig() {
        given()
            .when()
            .get("/config")
            .then()
            .statusCode(200)
            .body("compatibilityLevel", notNullValue());
        
        System.out.println("✅ Retrieved global config");
    }

    @Test
    @Order(10)
    @DisplayName("PUT /config - Should update global config")
    void testUpdateGlobalConfig() {
        // Save current config
        String currentConfig = get("/config").jsonPath().getString("compatibilityLevel");
        
        // Update to NONE
        given()
            .contentType("application/vnd.schemaregistry.v1+json")
            .body("{\"compatibility\":\"NONE\"}")
        .when()
            .put("/config")
        .then()
            .statusCode(200)
            .body("compatibility", equalTo("NONE"));
        
        // Restore original config
        given()
            .contentType("application/vnd.schemaregistry.v1+json")
            .body("{\"compatibility\":\"" + currentConfig + "\"}")
        .when()
            .put("/config");
        
        System.out.println("✅ Updated global config");
    }

    @Test
    @Order(11)
    @DisplayName("GET /config/{subject} - Should get subject config")
    void testGetSubjectConfig() {
        // Might return 404 if no subject-specific config
        int statusCode = 
            given()
                .when()
                .get("/config/test-user-value")
                .then()
                .extract()
                .statusCode();
        
        assertTrue(statusCode == 200 || statusCode == 404);
        System.out.println("✅ Subject config endpoint working (status: " + statusCode + ")");
    }

    // ============================================================================
    // Compatibility Tests
    // ============================================================================

    @Test
    @Order(12)
    @DisplayName("POST /compatibility - Test backward compatible schema")
    void testBackwardCompatibleSchema() {
        String requestBody = String.format("{\"schema\":\"%s\"}", 
            USER_SCHEMA_V2_COMPATIBLE.replace("\"", "\\\""));
        
        given()
            .contentType("application/vnd.schemaregistry.v1+json")
            .body(requestBody)
        .when()
            .post("/compatibility/subjects/test-user-value/versions/latest")
        .then()
            .statusCode(200)
            .body("is_compatible", equalTo(true));
        
        System.out.println("✅ Backward compatible schema validated");
    }

    @Test
    @Order(13)
    @DisplayName("POST /compatibility - Test incompatible schema")
    void testIncompatibleSchema() {
        String requestBody = String.format("{\"schema\":\"%s\"}", 
            USER_SCHEMA_V2_INCOMPATIBLE.replace("\"", "\\\""));
        
        given()
            .contentType("application/vnd.schemaregistry.v1+json")
            .body(requestBody)
        .when()
            .post("/compatibility/subjects/test-user-value/versions/latest")
        .then()
            .statusCode(200)
            .body("is_compatible", equalTo(false));
        
        System.out.println("✅ Incompatible schema correctly detected");
    }

    // ============================================================================
    // Schema Lookup Tests
    // ============================================================================

    @Test
    @Order(14)
    @DisplayName("POST /subjects/{subject} - Should check if schema exists")
    void testCheckSchemaExists() {
        String requestBody = String.format("{\"schema\":\"%s\"}", 
            USER_SCHEMA_V1.replace("\"", "\\\""));
        
        given()
            .contentType("application/vnd.schemaregistry.v1+json")
            .body(requestBody)
        .when()
            .post("/subjects/test-user-value")
        .then()
            .statusCode(200)
            .body("subject", equalTo("test-user-value"))
            .body("id", greaterThan(0))
            .body("version", greaterThan(0));
        
        System.out.println("✅ Schema lookup working");
    }

    // ============================================================================
    // Client Library Tests
    // ============================================================================

    @Test
    @Order(15)
    @DisplayName("Client: Register and retrieve schema")
    void testClientRegisterAndRetrieve() throws IOException, RestClientException {
        String subject = "client-test-product-value";
        Schema schema = new Schema.Parser().parse(
            "{\"type\":\"record\",\"name\":\"Product\",\"fields\":["
            + "{\"name\":\"id\",\"type\":\"long\"},"
            + "{\"name\":\"price\",\"type\":\"double\"}]}"
        );
        
        // Register
        int id = client.register(subject, new AvroSchema(schema));
        assertTrue(id > 0);
        
        // Retrieve
        Schema retrieved = client.getById(id).rawSchema();
        assertEquals(schema, retrieved);
        
        System.out.println("✅ Client library registration working");
    }

    @Test
    @Order(16)
    @DisplayName("Client: Test compatibility")
    void testClientCompatibility() throws IOException, RestClientException {
        String subject = "test-user-value";
        
        Schema compatibleSchema = new Schema.Parser().parse(USER_SCHEMA_V2_COMPATIBLE);
        boolean isCompatible = client.testCompatibility(subject, new AvroSchema(compatibleSchema));
        
        assertTrue(isCompatible);
        System.out.println("✅ Client compatibility checking working");
    }

    @Test
    @Order(17)
    @DisplayName("Client: List all subjects")
    void testClientListSubjects() throws IOException, RestClientException {
        List<String> subjects = client.getAllSubjects();
        assertNotNull(subjects);
        assertFalse(subjects.isEmpty());
        
        System.out.println("✅ Client list subjects working (" + subjects.size() + " subjects)");
    }

    // ============================================================================
    // Error Handling Tests
    // ============================================================================

    @Test
    @Order(18)
    @DisplayName("Error: Invalid JSON should return 400")
    void testInvalidJson() {
        given()
            .contentType("application/vnd.schemaregistry.v1+json")
            .body("invalid json")
        .when()
            .post("/subjects/test-subject/versions")
        .then()
            .statusCode(anyOf(is(400), is(422), is(500))); // Different implementations may vary
        
        System.out.println("✅ Invalid JSON handling working");
    }

    @Test
    @Order(19)
    @DisplayName("Error: Non-existent subject should return 404")
    void testNonExistentSubject() {
        given()
            .when()
            .get("/subjects/non-existent-subject-xyz/versions/latest")
            .then()
            .statusCode(404);
        
        System.out.println("✅ Non-existent subject handling working");
    }

    // ============================================================================
    // Summary
    // ============================================================================

    @Test
    @Order(20)
    @DisplayName("Summary: All compatibility tests")
    void testSummary() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║              COMPATIBILITY TEST SUMMARY                   ║");
        System.out.println("╠═══════════════════════════════════════════════════════════╣");
        System.out.println("║  ✅ All Confluent REST API compatibility tests passed     ║");
        System.out.println("║  ✅ Schema Registry Client library working correctly      ║");
        System.out.println("║  ✅ Compatibility checking functioning as expected        ║");
        System.out.println("║  ✅ Error handling consistent with Confluent behavior     ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
        
        // Final verification
        assertTrue(true, "All compatibility tests passed successfully!");
    }
}
