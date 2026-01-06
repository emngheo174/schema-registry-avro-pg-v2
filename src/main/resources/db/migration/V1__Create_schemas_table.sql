-- Create schemas table with deduplication support
CREATE TABLE schemas (
    id SERIAL PRIMARY KEY,
    schema_text TEXT NOT NULL,
    schema_type VARCHAR(50) NOT NULL DEFAULT 'AVRO',
    md5_hash VARCHAR(32) NOT NULL UNIQUE,
    references_json JSONB DEFAULT '[]'::jsonb,
    metadata_json JSONB,
    ruleset_json JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create subject_versions table for subject-version to schema mapping
CREATE TABLE subject_versions (
    id SERIAL PRIMARY KEY,
    subject VARCHAR(255) NOT NULL,
    version INT NOT NULL,
    schema_id INT NOT NULL REFERENCES schemas(id),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(subject, version)
);

-- Create indexes for performance
CREATE INDEX idx_schemas_md5_hash ON schemas(md5_hash);
CREATE INDEX idx_subject_versions_subject ON subject_versions(subject);
CREATE INDEX idx_subject_versions_schema_id ON subject_versions(schema_id);
CREATE INDEX idx_subject_versions_subject_version ON subject_versions(subject, version DESC);