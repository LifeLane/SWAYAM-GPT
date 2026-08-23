# Automation Engine & Background Routines

The `AutomationEngine` allows agents and users to define event-driven triggers and actions for ambient on-device intelligence.

## Trigger Types
- `SCHEDULED_TIME`: Periodic or time-of-day execution.
- `MEMORY_CREATED`: Fired when a new vector memory is stored.
- `BATTERY_LEVEL`: Battery thresholds (e.g. below 20%).
- `NETWORK_CHANGE`: WiFi / Cellular connectivity state transitions.
- `USER_PRESENCE`: Detected via MediaPipe face/pose perception.
- `LOCATION_ENTER` / `LOCATION_EXIT`: Geofence boundaries.

## Action Execution & Confirmation
Automations execute actions strictly through `ToolGateway`. If an automation action has `requiresConfirmation = true` or `riskLevel >= HIGH`, a `ToolActionProposal` is generated for user approval rather than running unattended.
