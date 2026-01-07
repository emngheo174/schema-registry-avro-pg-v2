-- Schema Registry Database Schema
-- Only supports AVRO schema type

-- Global configuration table
CREATE TABLE global_config (
    id SERIAL PRIMARY KEY,
    compatibility VARCHAR(50) NOT NULL DEFAULT 'BACKWARD',
    mode VARCHAR(50) NOT NULL DEFAULT 'READWRITE',
    compatibility_group VARCHAR(255),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert default global config
INSERT INTO global_config (compatibility, mode) VALUES ('BACKWARD', 'READWRITE');

-- Schemas table - stores unique schemas by MD5 hash
CREATE TABLE schemas (
    id SERIAL PRIMARY KEY,
    schema_text TEXT NOT NULL,
    schema_type VARCHAR(50) NOT NULL DEFAULT 'AVRO',
    md5_hash CHAR(32) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_schema_type CHECK (schema_type = 'AVRO')
);

-- Index for hash lookups
CREATE INDEX idx_schemas_md5_hash ON schemas(md5_hash);

-- Subject-version mappings
CREATE TABLE subject_versions (
    id BIGSERIAL PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL,
    schema_id INTEGER NOT NULL REFERENCES schemas(id),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_subject_version UNIQUE (subject, version)
);

-- Indexes for subject queries
CREATE INDEX idx_subject_versions_subject ON subject_versions(subject);
CREATE INDEX idx_subject_versions_schema_id ON subject_versions(schema_id);
CREATE INDEX idx_subject_versions_deleted ON subject_versions(deleted);

-- Subject-level configuration
CREATE TABLE subject_config (
    subject VARCHAR(255) PRIMARY KEY,
    compatibility VARCHAR(50),
    compatibility_group VARCHAR(255),
    mode VARCHAR(50),
    alias VARCHAR(255),
    normalize BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Schema references (for Avro named types)
CREATE TABLE schema_references (
    schema_id INTEGER NOT NULL REFERENCES schemas(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    version INTEGER NOT NULL,

    PRIMARY KEY (schema_id, name)
);

CREATE INDEX idx_schema_references_subject_version ON schema_references(subject, version);
