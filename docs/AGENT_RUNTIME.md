# Agent Runtime Orchestrator

The `AgentRuntime` powers autonomous multi-step reasoning and problem solving on device.

## Lifecycle & Step Execution Loop

1. **Context & Profile Resolution**:
   - Ingests natural language intent.
   - Loads `AgentProfile` (e.g. `ASSISTANT`, `MEMORY`, `VISION`, `PRODUCTIVITY`).
   - Retrieves relevant context from `MemoryStore`.
2. **Capability Discovery**:
   - Queries `CapabilityRegistry` to match intent with allowed tools.
3. **Multi-Step Reasoning Loop**:
   - For each step (up to `maxSteps`):
     - Formulates reasoning thought.
     - Selects candidate tool from discovered capabilities.
     - Dispatches through `ToolGateway`.
     - Collects `observation`.
     - Decides whether intent is fulfilled.
4. **Final Response Synthesis**:
   - Synthesizes all step observations into a concise user response.
   - Logs execution metrics (`latencyMs`, `tokensUsed`, `toolsExecuted`).
