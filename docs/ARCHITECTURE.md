# System Architecture

The EdgeAI Core Engine operates as a modular, privacy-preserving substrate designed for edge computing on Android devices.

## Architectural Layers

1. **Client / App Layer**:
   - Jetpack Compose interface adhering to the "Geometric Balance" design system.
   - One-line public API facades via `EdgeAICore`.

2. **Orchestration & Reasoning Layer**:
   - `AgentRuntime`: Coordinates the multi-step reasoning, tool dispatch, and observation loop.
   - `LiteRTLMEngine`: Executes prompt tokenization, KV-caching, and token generation on-device.
   - `CapabilityRegistry`: Discovers capabilities relevant to user intent while respecting `AgentProfile` boundaries.

3. **Security & Policy Layer**:
   - `ToolGateway`: Intercepts and validates every tool call before invocation.
   - `PolicyEngine`: Assesses risk levels (LOW, MEDIUM, HIGH, CRITICAL) and privacy constraints.
   - `ConfirmationManager`: Blocks high-risk actions until explicit user authorization is granted.
   - `PrivacyBoundary`: Enforces local-only containment, LAN private server access, or cloud fallback.

4. **Integration & Execution Layer**:
   - `McpClient` / `InternalMcpServer`: Implements JSON-RPC 2.0 based tool invocation over in-memory or remote transports.
   - `NativeToolDefinition`: Registered handlers for on-device actions (calendar, tasks, device telemetry, vision perception).
   - `MemoryStore`: Room-backed SQLite vector store with 384-dimensional cosine similarity indexing.
   - `AutomationEngine`: Event-driven trigger/action manager for autonomous background routines.
