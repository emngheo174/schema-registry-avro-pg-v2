-- Add subject config table
CREATE TABLE subject_configs (
    id SERIAL PRIMARY KEY,
    subject VARCHAR(255) NOT NULL UNIQUE,
    compatibility_level VARCHAR(50) NOT NULL DEFAULT 'BACKWARD',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);