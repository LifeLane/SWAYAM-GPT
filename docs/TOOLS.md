# Native Tools & Registry

EdgeAI Core provides a comprehensive suite of native tools registered within `ToolRegistry`.

## Default Tool Roster

| Tool ID | Name | Risk Level | Privacy Level | Description |
|---|---|---|---|---|
| `native.memory.search` | Search Memories | LOW | ON_DEVICE_ONLY | Queries vector database for semantic matches. |
| `native.memory.store` | Store Memory | MEDIUM | ON_DEVICE_ONLY | Persists new memory vector with category tags. |
| `native.vision.analyze_frame` | Analyze Camera Frame | LOW | ON_DEVICE_ONLY | Performs real-time vision perception via MediaPipe. |
| `native.calendar.get_events` | Get Calendar Events | LOW | ON_DEVICE_ONLY | Retrieves schedule items within a date window. |
| `native.calendar.create_event` | Create Calendar Event | HIGH | ON_DEVICE_ONLY | Creates a calendar entry (requires confirmation). |
| `native.tasks.get_tasks` | Get Tasks | LOW | ON_DEVICE_ONLY | Lists active tasks from the local task store. |
| `native.tasks.create_task` | Create Task | HIGH | ON_DEVICE_ONLY | Adds a task item (requires confirmation). |
| `native.weather.get_forecast` | Get Local Weather | LOW | PRIVATE_SERVER_PERMITTED | Retrieves weather forecast data. |
| `native.device.get_telemetry` | Get Device Status | LOW | ON_DEVICE_ONLY | Reads battery, RAM, and hardware metrics. |
| `native.notifications.post` | Post Notification | MEDIUM | ON_DEVICE_ONLY | Displays an on-device notification banner. |
| `native.automation.create_rule` | Create Automation | HIGH | ON_DEVICE_ONLY | Defines a persistent background event trigger. |
