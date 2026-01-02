-- Create schema_registry database
CREATE DATABASE schema_registry;

-- Switch to schema_registry
\c schema_registry;

-- Create schemas table
CREATE TABLE schemas (
    id SERIAL PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    version INT NOT NULL,
    schema_text TEXT NOT NULL,
    schema_type VARCHAR(50) NOT NULL DEFAULT 'AVRO',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(subject, version)
);