<?php
declare(strict_types=1);
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
ini_set('display_errors', '0');
function respond(int $status, array $body): void {
    http_response_code($status); echo json_encode($body, JSON_THROW_ON_ERROR); exit;
}
function field(array $data, string $key, string $pattern): string {
    $value = $data[$key] ?? null;
    if (!is_string($value) || !preg_match($pattern, $value)) {
        respond(400, ['ok' => false, 'error' => 'Invalid metadata']);
    }
    return $value;
}
function number(array $data, string $key, bool $nullable = false): ?int {
    $value = $data[$key] ?? null;
    if ($nullable && $value === null) return null;
    if (!is_int($value) || $value < 0) respond(400, ['ok' => false, 'error' => 'Invalid number']);
    return $value;
}
function flag(array $data, string $key): int {
    if (!isset($data[$key]) || !is_bool($data[$key])) respond(400, ['ok' => false, 'error' => 'Invalid flag']);
    return $data[$key] ? 1 : 0;
}
function query(PDO $db, string $sql, array $parameters): PDOStatement {
    $statement = $db->prepare($sql); $statement->execute($parameters); return $statement;
}
function upsert(PDO $db, array $data, string $device, bool $deleted): void {
    $uuid = field($data, 'uuid', '/^[a-f0-9-]{36}$/i');
    $hash = field($data, 'sha256', '/^[a-f0-9]{64}$/i');
    $values = [$device, $uuid, $hash, number($data, 'payload_bytes'), number($data, 'capacity_bytes'),
        flag($data, 'ghost'), flag($data, 'uses_passphrase'), number($data, 'created_at'),
        number($data, 'expires_at', true), number($data, 'updated_at'), number($data, 'revision'), $deleted ? 1 : 0];
    // Revision assignment comes last so each comparison uses the previous stored revision.
    query($db, 'INSERT INTO vaults (device_id,uuid,sha256,payload_bytes,capacity_bytes,ghost,uses_passphrase,
        created_at,expires_at,updated_at,revision,deleted) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        ON DUPLICATE KEY UPDATE sha256=IF(VALUES(revision)>=revision,VALUES(sha256),sha256),
        payload_bytes=IF(VALUES(revision)>=revision,VALUES(payload_bytes),payload_bytes),
        capacity_bytes=IF(VALUES(revision)>=revision,VALUES(capacity_bytes),capacity_bytes),
        ghost=IF(VALUES(revision)>=revision,VALUES(ghost),ghost),
        uses_passphrase=IF(VALUES(revision)>=revision,VALUES(uses_passphrase),uses_passphrase),
        expires_at=IF(VALUES(revision)>=revision,VALUES(expires_at),expires_at),
        updated_at=IF(VALUES(revision)>=revision,VALUES(updated_at),updated_at),
        deleted=IF(VALUES(revision)>=revision,VALUES(deleted),deleted), revision=GREATEST(revision,VALUES(revision))', $values);
}
function listVaults(PDO $db, array $data, string $device): void {
    $offset = number($data, 'offset');
    $statement = $db->prepare('SELECT uuid,sha256,payload_bytes,capacity_bytes,ghost,uses_passphrase,
        created_at,expires_at,updated_at,revision,deleted FROM vaults WHERE device_id=?
        ORDER BY uuid LIMIT 100 OFFSET ?');
    $statement->bindValue(1, $device); $statement->bindValue(2, $offset, PDO::PARAM_INT);
    $statement->execute();
    respond(200, ['ok' => true, 'vaults' => $statement->fetchAll(), 'next_offset' => $offset + 100]);
}
function logEvent(PDO $db, array $data, string $device): void {
    $event = field($data, 'event_id', '/^[a-zA-Z0-9-]{1,64}$/');
    $type = field($data, 'type', '/^(HIDE|REVEAL_OK|REVEAL_FAIL|AUTOLOCK|EXPORT|AIRPLANE_ON|AIRPLANE_OFF)$/');
    $uuid = ($data['vault_uuid'] ?? null) === null ? null : field($data, 'vault_uuid', '/^[a-f0-9-]{36}$/i');
    query($db, 'INSERT IGNORE INTO events(device_id,event_id,vault_uuid,type,ts) VALUES(?,?,?,?,?)',
        [$device, $event, $uuid, $type, number($data, 'ts')]);
    respond(200, ['ok' => true]);
}
function stats(PDO $db, string $device): void {
    $vaults = query($db, 'SELECT COUNT(*) AS vaults, COALESCE(SUM(payload_bytes),0) AS payload_bytes
        FROM vaults WHERE device_id=? AND deleted=0', [$device])->fetch();
    $events = query($db, 'SELECT COUNT(*) FROM events WHERE device_id=?', [$device])->fetchColumn();
    respond(200, ['ok' => true, 'vaults' => (int)$vaults['vaults'],
        'payload_bytes' => (int)$vaults['payload_bytes'], 'events' => (int)$events]);
}
try {
    if ($_SERVER['REQUEST_METHOD'] !== 'POST') respond(405, ['ok' => false, 'error' => 'Use POST JSON']);
    $config = require __DIR__ . '/config.php';
    $key = $_SERVER['HTTP_X_API_KEY'] ?? '';
    if (!is_string($config['api_key']) || strlen($config['api_key']) < 24
            || $config['api_key'] === 'REPLACE_WITH_A_LONG_RANDOM_SECRET'
            || !hash_equals($config['api_key'], $key)) respond(401, ['ok' => false, 'error' => 'Unauthorized']);
    $body = file_get_contents('php://input', false, null, 0, 65537);
    if ($body === false || strlen($body) > 65536) respond(413, ['ok' => false, 'error' => 'Request too large']);
    $data = json_decode($body, true, 32, JSON_THROW_ON_ERROR);
    if (!is_array($data)) respond(400, ['ok' => false, 'error' => 'JSON object required']);
    $device = field($data, 'device_id', '/^[a-f0-9-]{36}$/i');
    $db = new PDO($config['dsn'], $config['db_user'], $config['db_password'], [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_EMULATE_PREPARES => false,
        PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC]);
    switch ($data['action'] ?? '') {
        case 'health':
            query($db, 'SELECT 1', []);
            respond(200, ['ok' => true, 'service' => 'kinetic-vault', 'version' => 1]); break;
        case 'upsert_vault':
        case 'delete_vault':
            upsert($db, $data, $device, $data['action'] === 'delete_vault');
            respond(200, ['ok' => true]); break;
        case 'list_vaults': listVaults($db, $data, $device); break;
        case 'log_event': logEvent($db, $data, $device); break;
        case 'stats': stats($db, $device); break;
        default: respond(400, ['ok' => false, 'error' => 'Unknown action']);
    }
} catch (JsonException $error) {
    respond(400, ['ok' => false, 'error' => 'Invalid JSON']);
} catch (Throwable $error) {
    respond(503, ['ok' => false, 'error' => 'Ledger unavailable']);
}
