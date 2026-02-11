# Sử dụng System Tests của Confluent để test Custom Schema Registry

## 🎯 Mục tiêu

Chạy system tests từ dự án Confluent Schema Registry để verify rằng custom implementation của bạn tương thích với Confluent Schema Registry.

## 📋 Các bước thực hiện

### Bước 1: Clone Confluent Schema Registry repository

```bash
# Clone repo
git clone https://github.com/confluentinc/schema-registry.git
cd schema-registry

# Checkout version tương ứng với implementation của bạn
git checkout v7.6.0  # Hoặc version bạn đang implement
```

### Bước 2: Tìm system tests

```bash
# System tests thường nằm ở:
cd schema-registry/core/src/test/java/io/confluent/kafka/schemaregistry

# Hoặc
find . -name "*SystemTest*" -o -name "*IntegrationTest*"
```

### Bước 3: Cấu hình để test với Custom Schema Registry

Tạo file `test-config.properties`:

```properties
# Schema Registry URL - trỏ đến custom SR của bạn
schema.registry.url=http://localhost:8081

# Kafka bootstrap servers (nếu cần)
bootstrap.servers=localhost:9092

# Other settings
kafkastore.connection.url=localhost:2181
```

### Bước 4: Chạy specific tests

```bash
# Chạy tất cả integration tests
mvn test -pl core

# Hoặc chạy specific test class
mvn test -pl core -Dtest=RestApiTest

# Với custom config
mvn test -pl core -Dtest=RestApiTest \
  -Dschema.registry.url=http://localhost:8081
```

## 🔧 Cách tốt hơn: Tạo Maven module riêng

### Option 1: Tạo module test trong dự án của bạn

```bash
# Trong dự án schema-registry-avro-pg của bạn
mkdir -p confluent-compatibility-tests
cd confluent-compatibility-tests
```

Tạo `pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.confluent.schemaregistry</groupId>
    <artifactId>confluent-compatibility-tests</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <name>Confluent Schema Registry Compatibility Tests</name>

    <properties>
        <java.version>17</java.version>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <confluent.version>7.6.0</confluent.version>
        
        <!-- Custom SR URL -->
        <schema.registry.url>http://localhost:8081</schema.registry.url>
    </properties>

    <repositories>
        <repository>
            <id>confluent</id>
            <url>https://packages.confluent.io/maven/</url>
        </repository>
    </repositories>

    <dependencies>
        <!-- Confluent Schema Registry core (chứa tests) -->
        <dependency>
            <groupId>io.confluent</groupId>
            <artifactId>kafka-schema-registry</artifactId>
            <version>${confluent.version}</version>
            <classifier>tests</classifier>
            <scope>test</scope>
        </dependency>

        <!-- Schema Registry Client -->
        <dependency>
            <groupId>io.confluent</groupId>
            <artifactId>kafka-schema-registry-client</artifactId>
            <version>${confluent.version}</version>
        </dependency>

        <!-- Avro -->
        <dependency>
            <groupId>org.apache.avro</groupId>
            <artifactId>avro</artifactId>
            <version>1.11.3</version>
        </dependency>

        <!-- JUnit -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>5.10.1</version>
            <scope>test</scope>
        </dependency>

        <!-- REST Assured for API testing -->
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <version>5.4.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.2.2</version>
                <configuration>
                    <systemPropertyVariables>
                        <schema.registry.url>${schema.registry.url}</schema.registry.url>
                    </systemPropertyVariables>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

### Option 2: Sử dụng Confluent's test utilities

Thay vì chạy toàn bộ system tests, bạn có thể:

1. **Copy specific test classes** từ Confluent repo
2. **Adapt chúng** để test custom SR
3. **Run trong dự án của bạn**

## 📝 Ví dụ: Adapt Confluent tests

Tạo file test adapter:

```java
package io.confluent.schemaregistry.compatibility;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.junit.jupiter.api.BeforeAll;

/**
 * Base class để chạy Confluent tests với custom Schema Registry
 */
public abstract class ConfluentCompatibilityTestBase {
    
    protected static SchemaRegistryClient client;
    protected static final String SCHEMA_REGISTRY_URL = 
        System.getProperty("schema.registry.url", "http://localhost:8081");
    
    @BeforeAll
    public static void setupClient() {
        client = new CachedSchemaRegistryClient(SCHEMA_REGISTRY_URL, 100);
        System.out.println("Testing Schema Registry at: " + SCHEMA_REGISTRY_URL);
    }
}
```

## 🚀 Cách chạy nhanh nhất

### Sử dụng Docker Compose để chạy Confluent tests

Tạo `docker-compose.test.yml`:

```yaml
version: '3.8'

services:
  # Custom Schema Registry của bạn
  custom-schema-registry:
    image: schema-registry-avro-pg-v2-app
    ports:
      - "8081:8081"
    depends_on:
      - postgres
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/schema_registry
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres

  postgres:
    image: postgres:13
    environment:
      POSTGRES_DB: schema_registry
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres

  # Confluent test runner
  confluent-tests:
    image: maven:3.9-eclipse-temurin-17
    volumes:
      - ./confluent-compatibility-tests:/tests
      - ~/.m2:/root/.m2
    working_dir: /tests
    command: >
      sh -c "
        mvn clean test -Dschema.registry.url=http://custom-schema-registry:8081
      "
    depends_on:
      - custom-schema-registry
```

Chạy:

```bash
docker-compose -f docker-compose.test.yml up confluent-tests
```

## 📊 Test Coverage cần có

Để pass Confluent compatibility tests, custom SR của bạn cần support:

### Core REST API Endpoints ✅

```
✅ GET    /
✅ GET    /subjects
✅ GET    /subjects/{subject}/versions
✅ GET    /subjects/{subject}/versions/{version}
✅ GET    /subjects/{subject}/versions/latest
✅ POST   /subjects/{subject}/versions
✅ POST   /subjects/{subject}
✅ DELETE /subjects/{subject}
✅ DELETE /subjects/{subject}/versions/{version}
✅ GET    /schemas/ids/{id}
✅ GET    /config
✅ PUT    /config
✅ GET    /config/{subject}
✅ PUT    /config/{subject}
✅ POST   /compatibility/subjects/{subject}/versions/{version}
✅ POST   /compatibility/subjects/{subject}/versions/latest
```

### Schema Types Support ✅

```
✅ AVRO
❌ JSON Schema (not in your impl)
❌ Protobuf (not in your impl)
```

### Compatibility Modes ✅

```
✅ BACKWARD
✅ FORWARD
✅ FULL
✅ NONE
✅ BACKWARD_TRANSITIVE
✅ FORWARD_TRANSITIVE
✅ FULL_TRANSITIVE
```

## 🧪 Recommended Test Strategy

### Phase 1: API Compatibility Tests
Test tất cả REST endpoints với same request/response format như Confluent.

### Phase 2: Schema Registry Client Tests
Test với official Confluent client library.

### Phase 3: Serializer/Deserializer Tests
Test với Avro serializers.

### Phase 4: Compatibility Logic Tests
Test tất cả compatibility modes.

## 📁 Suggested Project Structure

```
your-project/
├── src/main/java/              # Custom SR implementation
├── src/test/java/
│   ├── unit/                   # Unit tests
│   └── integration/            # Your integration tests
└── confluent-compatibility-tests/
    ├── pom.xml
    └── src/test/java/
        ├── api/                # REST API compatibility tests
        ├── client/             # Client library tests
        └── compatibility/      # Schema compatibility tests
```

## 🎯 Quick Start Script

```bash
#!/bin/bash

# setup-confluent-tests.sh

echo "Setting up Confluent compatibility tests..."

# 1. Clone Confluent SR (if not exists)
if [ ! -d "confluent-schema-registry" ]; then
    git clone https://github.com/confluentinc/schema-registry.git confluent-schema-registry
    cd confluent-schema-registry
    git checkout v7.6.0
    cd ..
fi

# 2. Create test module
mkdir -p confluent-compatibility-tests/src/test/java

# 3. Copy relevant test files
cp confluent-schema-registry/core/src/test/java/io/confluent/kafka/schemaregistry/rest/RestApiTest.java \
   confluent-compatibility-tests/src/test/java/

# 4. Run tests against your SR
cd confluent-compatibility-tests
mvn test -Dschema.registry.url=http://localhost:8081

echo "Done!"
```

## 💡 Tips

1. **Start small**: Bắt đầu với basic REST API tests
2. **Incremental**: Thêm tests từng phần một
3. **Document differences**: Note bất kỳ differences nào với Confluent
4. **CI/CD**: Add tests vào pipeline

## ⚠️ Lưu ý quan trọng

- Confluent tests có thể assume Kafka backend → bạn dùng PostgreSQL
- Một số advanced features có thể khác
- Performance characteristics sẽ khác
- Cần adapt tests cho PostgreSQL-specific behavior

---

**Kết luận**: Bạn hoàn toàn có thể dùng Confluent tests, nhưng cần adapt chúng cho PostgreSQL backend của bạn!
