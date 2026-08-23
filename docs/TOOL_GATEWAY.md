# Tool Gateway & Execution Isolation

The `ToolGateway` is the single enforcement choke point for all tool invocations in EdgeAI Core.

## Execution Flow

```
Agent / LLM Intent
        │
        ▼
ToolGateway.execute(toolId, args, consent)
        │
        ▼
PolicyEngine.evaluateToolExecution()
        │
   ┌────┴───────────────────────────┐
   │ Allowed?                       │
  YES                               NO
   │                                 │
   ▼                                 ▼
Risk >= HIGH? ─────────► Create ToolActionProposal
   │                     (Await user authorization)
  NO
   │
   ▼
ToolRegistry.find(toolId)
   │
   ▼
ToolExecutionResult
   │
   ▼
AuditLog Recording
```

## Security Invariants
- **No Direct API Access**: LLMs cannot invoke Android platform SDKs directly.
- **Audit Trace**: Every tool execution is logged with timestamp, tool ID, risk level, duration, and success status.
- **Pre-Execution Validation**: Checks tool availability, enabled status, parameter schemas, and network permission gates.
