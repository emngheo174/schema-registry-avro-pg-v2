# Schema Registry

A Java-based Schema Registry for Avro schemas using PostgreSQL.

## Features

- Store and retrieve Avro schemas
- Schema validation
- Versioning
- REST API
- Unit tests, performance tests, security tests, system tests, migration tests

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