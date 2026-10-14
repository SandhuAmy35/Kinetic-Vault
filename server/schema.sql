CREATE DATABASE IF NOT EXISTS kinetic_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kinetic_vault;
CREATE TABLE IF NOT EXISTS vaults (
 device_id CHAR(36) NOT NULL, uuid CHAR(36) NOT NULL, sha256 CHAR(64) NOT NULL,
 payload_bytes BIGINT UNSIGNED NOT NULL, capacity_bytes BIGINT UNSIGNED NOT NULL,
 ghost BOOLEAN NOT NULL, uses_passphrase BOOLEAN NOT NULL,
 created_at BIGINT UNSIGNED NOT NULL, expires_at BIGINT UNSIGNED NULL,
 updated_at BIGINT UNSIGNED NOT NULL, revision BIGINT UNSIGNED NOT NULL,
 deleted BOOLEAN NOT NULL DEFAULT FALSE, PRIMARY KEY(device_id, uuid)
);
CREATE TABLE IF NOT EXISTS events (
 device_id CHAR(36) NOT NULL, event_id VARCHAR(64) NOT NULL, vault_uuid CHAR(36) NULL,
 type VARCHAR(24) NOT NULL, ts BIGINT UNSIGNED NOT NULL,
 PRIMARY KEY(device_id, event_id), INDEX events_device_time(device_id, ts)
);
