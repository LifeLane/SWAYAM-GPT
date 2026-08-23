# Security Architecture & Risk Control

Security in EdgeAI Core is built on the principle of least privilege and mandatory confirmation.

## Risk Classification Hierarchy

- **LOW Risk**: Read-only queries (e.g. read battery status, search local memory, get weather). Executed automatically.
- **MEDIUM Risk**: Non-destructive state writes (e.g. store new memory, show notification). Executed with audit log entry.
- **HIGH Risk**: State-altering operations (e.g. create calendar events, add persistent automation rules, delete records). Requires human-in-the-loop confirmation.
- **CRITICAL Risk**: Dangerous operations (e.g. wipe database, execute external code, transmit private keys). Always requires explicit confirmation and cannot be bypassed.

## Tamper-Evident Audit Vault
All policy evaluations, tool executions, and provider dispatches are logged to an append-only audit trail with timestamps and execution hashes.
