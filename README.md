# Schema Registry

A Java-based Schema Registry for Avro schemas using PostgreSQL, inspired by Confluent Schema Registry.

## Features

- Store and retrieve Avro schemas
- Schema validation and compatibility checking
- Versioning
- Subject-level and global compatibility levels
- REST API
- Comprehensive tests (unit, integration, performance, security, system, migration)

## API Endpoints

### Schemas
- `POST /subjects/{subject}` - Register a new schema
- `GET /subjects/{subject}/versions/{version}` - Get schema by version
- `GET /subjects/{subject}/versions` - Get all versions
- `GET /subjects/{subject}` - Get latest schema
- `DELETE /subjects/{subject}/versions/{version}` - Delete schema version
- `DELETE /subjects/{subject}` - Delete entire subject

### Subjects
- `GET /subjects` - List all subjects

### Compatibility
- `POST /subjects/{subject}/compatibility` - Check if schema is compatible
- `GET /subjects/{subject}/config` - Get subject compatibility level
- `PUT /subjects/{subject}/config` - Update subject compatibility level
- `GET /subjects/config` - Get global compatibility level

### Monitoring
- `GET /actuator/health` - Health check
- `GET /actuator/info` - Application info
- `GET /actuator/metrics` - Metrics

## Requirements

- Docker and Docker Compose

## Build and Run with Docker

```bash
docker-compose up --build
```

The application will be available at http://localhost:8080

## Manual Build (requires Java 17 and Maven)

```bash
mvn clean compile
```

## Test

```bash
mvn test
```

## Run

```bash
mvn spring-boot:run
```