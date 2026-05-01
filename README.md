# Distributed Tic Tac Toe Microservices

This repository is prepared as a Gradle multi-module Spring Boot project for a backend home assignment.

Business logic, session simulation, persistence, infrastructure, and UI implementation are intentionally not implemented yet.

## Project Structure

```text
TicTac/
├── build.gradle
├── settings.gradle
├── game-engine-service/
│   └── src/
│       ├── main/java/com/example/tictac/engine/
│       │   ├── config/
│       │   ├── controller/
│       │   ├── exception/
│       │   ├── model/
│       │   ├── repository/
│       │   └── service/
│       └── main/resources/application.properties
├── game-session-service/
│   └── src/
│       ├── main/java/com/example/tictac/session/
│       │   ├── config/
│       │   ├── controller/
│       │   ├── exception/
│       │   ├── model/
│       │   ├── repository/
│       │   └── service/
│       └── main/resources/application.properties
└── ui/
    └── src/
```

## Modules

| Module | Type | Default Port |
| --- | --- | --- |
| `game-engine-service` | Spring Boot backend service | `8081` |
| `game-session-service` | Spring Boot backend service | `8082` |
| `ui` | React TypeScript frontend (Vite) | `3000` |

## Build All Modules

```bash
./gradlew clean build
```

## Run Backend Services

Run the game engine service:

```bash
./gradlew :game-engine-service:bootRun
```

It starts on `http://localhost:8081`.

Run the game session service:

```bash
./gradlew :game-session-service:bootRun
```

It starts on `http://localhost:8082`.

## Run the UI

Requires Node.js 18+.

```bash
cd ui
npm install
npm run dev
```

The dev server starts on `http://localhost:3000` and proxies REST and WebSocket requests to the game session service at port 8082.

## Health Checks

The backend services include minimal startup verification endpoints:

```text
GET http://localhost:8081/health
GET http://localhost:8082/health
```

These endpoints exist only to verify that each service starts independently.

## WebSocket (Real-Time Simulation Updates)

The game session service publishes real-time events over STOMP/WebSocket as a simulation runs.

### Endpoint

- **WebSocket URL:** `ws://localhost:8082/ws` (native WebSocket) or `http://localhost:8082/ws` (SockJS fallback)
- **Topic:** `/topic/sessions/{sessionId}`

### Event Payload

After each move and at simulation completion/failure, an event is published:

```json
{
  "session-id": "...",
  "game-id": "...",
  "session-status": "SIMULATING | COMPLETED | FAILED",
  "current-game-state": { "game-id": "...", "board": [...], "status": "...", "winner": null, "next-player": "..." },
  "latest-move": { "move-number": 1, "player": "X", "position": 4, "resulting-status": "IN_PROGRESS", "board-after-move": [...], "created-at": "..." },
  "move-history": [ ... ],
  "failure-reason": null
}
```

### How It Works

1. The React UI creates a session via `POST /sessions`.
2. It subscribes to `/topic/sessions/{sessionId}` over WebSocket.
3. It fires `POST /sessions/{sessionId}/simulate` (fire-and-forget).
4. The backend publishes an event after each move is applied by the Game Engine.
5. The UI updates the board, move history, and status live from each event.
6. A final event is published when the game reaches a terminal state (COMPLETED) or an error occurs (FAILED).

### REST API (unchanged)

| Method | Path | Description |
| --- | --- | --- |
| POST | `/sessions` | Create a new game session |
| POST | `/sessions/{sessionId}/simulate` | Trigger automated simulation |
| GET | `/sessions/{sessionId}` | Get current session state |