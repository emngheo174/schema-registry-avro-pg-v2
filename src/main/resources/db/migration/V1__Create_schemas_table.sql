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

-- Create index for faster subject lookups
CREATE INDEX idx_schemas_subject ON schemas(subject);

-- Create index for faster subject and version lookups
CREATE INDEX idx_schemas_subject_version ON schemas(subject, version DESC);