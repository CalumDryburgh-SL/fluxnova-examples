# FluxNova MCP Query Demo

A demo showing how to query FluxNova BPM process data using an MCP server and an AI chatbot powered by OpenAI.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose
- An OpenAI API key

## Quick Start

```bash
OPENAI_API_KEY=sk-... docker compose up --build
```

Once all services are healthy, open [http://localhost:3000](http://localhost:3000) in your browser.

## Services

| Service          | URL                   | Description             |
| ---------------- | --------------------- | ----------------------- |
| demo-ui          | http://localhost:3000 | Chat UI                 |
| mcp-client       | http://localhost:8083 | AI chatbot / MCP client |
| fluxnova-service | http://localhost:8084 | BPM engine + MCP server |
