# Model Context Protocol (MCP) Integration

EdgeAI Core implements standard Model Context Protocol (MCP) JSON-RPC specifications for tool discovery and invocation.

## Protocol Structure

MCP communication adheres to JSON-RPC 2.0:
- **Initialize Request / Response**: Negotiates protocol version and server capabilities (`tools`, `resources`, `prompts`).
- **Tools List (`tools/list`)**: Discovers available tools, argument JSON schemas, and descriptions.
- **Tool Call (`tools/call`)**: Executes a specific tool with arguments and returns structured content chunks.

## Architecture

1. **`InternalMcpServer`**:
   - A built-in local MCP server operating over an in-memory loopback transport (`InMemoryMcpTransport`).
   - Exposes device telemetry (`get_device_metrics`), weather queries (`get_local_weather`), and local system services.

2. **`McpClient`**:
   - Manages connections to multiple MCP servers.
   - Converts standard MCP tool schemas into `ToolGateway` endpoints.

3. **`McpSecurityManager`**:
   - Assigns each server a trust rating:
     - `TRUSTED_LOCAL`: Local in-process loopback.
     - `TRUSTED_PRIVATE`: User-verified private LAN / VPN endpoints with TLS.
     - `USER_APPROVED_REMOTE`: Remote server with explicit per-session approval.
     - `UNTRUSTED`: Blocked from executing any native system tools.
