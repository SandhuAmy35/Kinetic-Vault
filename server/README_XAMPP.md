# Kinetic Vault Cloud Ledger

Use XAMPP with PHP 8.0 or newer, a 64-bit PHP runtime, MySQL/MariaDB and PDO MySQL enabled. Start Apache and MySQL in the XAMPP control panel. Copy this server directory to C:\xampp\htdocs\kinetic-vault and import schema.sql in phpMyAdmin. Copy config.sample.php to config.php, set the database credentials, and replace api_key with a random secret of at least 24 characters. The supplied placeholder is deliberately rejected. Use a dedicated database user for deployed servers. Keep config.php out of source control; Apache must honor the included .htaccess so configuration and schema files cannot be downloaded.

In Android Settings, enter the full endpoint and the same API key, then Save settings. The emulator reaches the PC at http://10.0.2.2/kinetic-vault/api.php. A real phone needs the PC LAN IP, for example http://192.168.1.100/kinetic-vault/api.php, with the PC and phone on the same LAN and Apache allowed through the private-network firewall. localhost on the phone means the phone itself.

The debug resource app/src/debug/res/xml/network_security_config.xml permits HTTP only for explicitly named development hosts. Replace 192.168.1.100 with your actual PC LAN IP before using a real phone. Android cannot allow an entire LAN address range with this domain rule. The release policy accepts HTTPS only. Plain HTTP exposes the development API key and metadata to the LAN; use HTTPS and a different API key for deployment.

## JSON protocol

Every action is a POST to api.php with Content-Type: application/json and X-API-KEY. All requests include action and a random device_id UUID. Responses use an ok boolean; errors carry a generic error string and an HTTP error status. Requests and responses are limited to 64 KiB by the Android client/server input. Millisecond timestamps and byte counts are nonnegative integers; ghost and uses_passphrase are JSON booleans. The endpoint never accepts files or images into storage.

health accepts the common fields and returns service=kinetic-vault, version=1 after connecting to the database.

upsert_vault requires uuid, sha256, payload_bytes, capacity_bytes, ghost, uses_passphrase, created_at, expires_at (integer or null), updated_at and revision. delete_vault uses the same metadata envelope and stores a tombstone, preventing a delayed older revision from restoring a deleted entry. Revisions are monotonic per local vault.

list_vaults requires offset (start at 0) and returns up to 100 metadata rows and next_offset. It includes tombstones. This action is for metadata inspection; the Android app does not download or restore vault images.

log_event requires event_id, vault_uuid (UUID or null), type and ts. Supported types are HIDE, REVEAL_OK, REVEAL_FAIL, AUTOLOCK, EXPORT, AIRPLANE_ON and AIRPLANE_OFF. Device-scoped event IDs make retries idempotent. Airplane events have a null vault UUID.

stats returns vaults (nondeleted count), payload_bytes (nondeleted sum) and events for the requesting device. This is a single configured ledger using a shared API key, not a user-account service; device IDs partition records but are not independent authentication credentials.

## Offline behavior

Local Hide, Reveal and Vault features do not require the server. SQLite sync_state is 0=pending, 1=synced, 2=failed; failed rows remain queued. Events use a separate persistent outbox. Successful vault acknowledgements match the sent revision so concurrent local edits stay pending. Retry timing is persisted and doubles from 5 seconds to a 5-minute cap. Pending batches continue while the app is visible; no background service or new dependency is required. Retry sync and saving server settings reset backoff. Changing the URL or API key queues metadata again for the new destination.

The Home Cloud Ledger shows Synced, Pending or Offline, queued update count and server totals. Cached totals are identified as last received. Settings Test Connection uses entered values without saving them. Connection errors offer recovery while all local features remain available. Airplane broadcasts pause uploads, show the Home banner and enqueue an event; leaving airplane mode resumes retries. The receiver is registered only while an activity is visible and removed when the app stops.

Only UUIDs, SHA-256, sizes, mode flags, timestamps, revisions, event types and a random installation device ID are sent. No local titles, secret filenames, image URIs, thumbnails, image bytes, location, keys, gestures, passphrases, salts, IVs or plaintext are uploaded. The API key is an authentication header, separate from encryption credentials. Metadata sync is not an encrypted-image backup.
