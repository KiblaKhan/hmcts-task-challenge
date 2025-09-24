-- Avoid reserved words like "key"
CREATE TABLE IF NOT EXISTS idempotency_entry (
  id_key      VARCHAR(255) PRIMARY KEY,
  fingerprint VARCHAR(255)
);

