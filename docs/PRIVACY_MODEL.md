# Privacy Model & Data Containment

EdgeAI Core guarantees absolute local-first data containment through strict cryptographic and architectural boundaries.

## Tri-Tier Containment Rules

1. **Local-Only (Default)**:
   - Raw camera frames, face/pose landmarks, audio signals, and vector embeddings NEVER leave device volatile memory/SQLite.
   - All inference occurs on device via LiteRT.
2. **Private Server (User-Owned LAN / VPN)**:
   - Requires explicit user enablement in Privacy Center.
   - Restricts communication to user-specified IP endpoints with TLS certificate validation.
3. **Public Cloud Fallback**:
   - Strictly off by default.
   - Sensitive personal memories, health data, and camera streams are blocked by `PolicyEngine` from ever reaching cloud endpoints.
   - Requires explicit, per-request consent for non-sensitive public queries.
