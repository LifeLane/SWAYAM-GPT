# Private AI Server Setup Guide

EdgeAI Core can offload heavy inference to a user-owned local AI server running in their home or office network.

## Supported Server Backends
- **vLLM** (OpenAI-compatible `/v1/chat/completions`)
- **Ollama** (`/api/generate` / `/v1/chat/completions`)
- **SGLang**
- **FastAPI / TGI** custom inference endpoints

## Setup Instructions

1. Start your local inference server:
   ```bash
   vllm serve google/gemma-2-9b-it --port 8000 --host 0.0.0.0
   ```
2. In the EdgeAI Core Android app:
   - Navigate to **Privacy Center** or **AI Engine Inspector**.
   - Enable **Private AI Server**.
   - Configure Endpoint URL (e.g., `https://192.168.1.100:8000`) and API Key if configured.
   - Run the built-in Connection & Health Check probe.
