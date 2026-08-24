# EdgeAI Core Engine & Autonomous Agent Subsystem

A secure, modular, privacy-first Edge AI Agent Engine for Android. Built with Kotlin, Jetpack Compose, LiteRT, LiteRT-LM (Gemma 2B INT4), MediaPipe perception, Model Context Protocol (MCP), Tool Gateways, and Tri-Tier AI routing (Local → Private LAN → Public Cloud).

---

## 🏛️ Architecture Overview

```
                          ┌──────────────────────────┐
                          │   Android Application    │
                          │   (or Developer Agent)   │
                          └─────────────┬────────────┘
                                        │
                                        ▼
    ┌───────────────────────────────────────────────────────────────────────┐
    │                            EdgeAICore Facade                          │
    │  edgeAI.agent.run(request = "...", profile = AgentProfile.MEMORY)     │
    └───────┬───────────────────────────┬───────────────────────────┬───────┘
            │                           │                           │
            ▼                           ▼                           ▼
 ┌──────────────────────┐    ┌──────────────────────┐    ┌──────────────────────┐
 │    Agent Runtime     │    │     Tool Gateway     │    │   Privacy Boundary   │
 │ (Intent → Reasoning  │    │  (Sandbox Isolation  │    │   (Local / Private   │
 │  → Observation Loop) │    │  & Policy Execution) │    │   / Public Fallback) │
 └──────────┬───────────┘    └──────────┬───────────┘    └──────────┬───────────┘
            │                           │                           │
            ▼                           ▼                           ▼
 ┌──────────────────────┐    ┌──────────────────────┐    ┌──────────────────────┐
 │     MCP Client       │    │ Capability Registry  │    │  Automation Engine   │
 │  (Tool Discovery &   │    │  (Intent Matching &  │    │ (Triggers & Actions  │
 │  JSON-RPC Transport) │    │   Profile Filtering) │    │   With Risk Gates)   │
 └──────────────────────┘    └──────────────────────┘    └──────────────────────┘
```

---

## 🚀 Engine Capabilities

1. **LiteRT-LM On-Device LLM**: Local language reasoning powered by Gemma 2B INT4 running on CPU/GPU/NPU.
2. **MediaPipe Vision Pipeline**: Real-time camera perception (Pose, Face, Hand, Object detection) processed strictly in RAM with zero disk persistence.
3. **Model Context Protocol (MCP)**: Dynamic tool discovery and execution conforming to the standard MCP specification.
4. **Tool Gateway**: Strict capability gate preventing any LLM or Agent from directly touching Android APIs without policy validation and risk confirmation.
5. **Multi-Step Autonomous Agent**: Reasoning loop capable of context retrieval, capability discovery, multi-step execution, and final synthesis.
6. **Tri-Tier Intelligence Routing**:
   - **Local AI** (Default): 100% on-device sandbox.
   - **Private AI**: HTTPS / TLS-pinned LAN server (vLLM, Ollama, SGLang).
   - **Public Cloud AI**: Explicitly consented Gemini fallback for public queries.
7. **Local Vector Memory**: On-device Room SQLite database with 384-dimensional cosine similarity indexing.
8. **Tamper-Evident Audit Trail**: Cryptographically structured audit logging of all inference calls and tool executions.

---

## 💻 Quickstart

### Basic Agent Invocation
```kotlin
val edgeAI = EdgeAICore.getInstance(context)

// One-line autonomous agent execution
val result = edgeAI.agent.run(
    request = "Schedule focus time and summarize my latest notes",
    profile = AgentProfile.PRODUCTIVITY,
    userConsentGiven = true
)

println("Agent Result: ${result.finalResponse}")
println("Tools Executed: ${result.toolsExecuted}")
```

### Direct Tool Gateway Execution
```kotlin
val executionResult = edgeAI.gateway.execute(
    toolId = "native.tasks.get_tasks",
    arguments = mapOf("filter" to "active"),
    userConsentGiven = true
)
```

### Local Memory Recall
```kotlin
edgeAI.memory.remember("Project roadmap discussion points", category = "work")
val relevantMemories = edgeAI.memory.recall("roadmap")
```

---

## 🔒 Privacy Model & Security Practices

- **Zero Silent Transmission**: No payload leaves the device without satisfying privacy boundary rules and policy evaluations.
- **Mandatory Human-in-the-Loop**: High and Critical risk actions (e.g., deleting data, sending messages, setting system alarms) produce an explicit confirmation proposal before execution.
- **Scoped Tool Access**: LLMs never receive raw API handles; all operations execute through typed schema definitions inside `ToolGateway`.
- **MCP Trust Boundaries**: Every MCP server is assigned a trust level (`TRUSTED_LOCAL`, `TRUSTED_PRIVATE`, `USER_APPROVED_REMOTE`, `UNTRUSTED`).

---

## 📦 Google Play Release & Permanent Signing

Release artifacts (APK and Google Play App Bundle `.aab`) are signed using a permanent upload key stored securely as GitHub Secrets.

### Required GitHub Secrets
- `KEYSTORE_BASE64`: Base64-encoded string of the permanent upload keystore (`my-upload-key.jks`).
- `STORE_PASSWORD`: Keystore password.
- `KEY_PASSWORD`: Private key alias password (`upload`).

### Automated Artifacts
- **`swayam-gpt-aab`**: Signed Android App Bundle ready for Google Play Console upload.
- **`swayam-gpt-apk`**: Signed release APK for testing.

For detailed step-by-step instructions on generating, encoding, and managing the permanent upload key, see [Google Play Release Guide](docs/PLAY_STORE_RELEASE.md).

For detailed documentation, see the [docs/](docs/) directory.
