# Distributed Tic Tac Toe Microservices

A distributed Tic Tac Toe application built as a Gradle multi-module Spring Boot project. The system implements a complete game engine with move validation and win/draw detection, a session orchestration service that can simulate full games automatically or host human-vs-computer matches, and a React frontend that displays games in real time over WebSocket.

## Architecture

```text
┌─────────────┐         ┌──────────────────────┐         ┌──────────────────┐
│   React UI  │──REST──▶│  Game Session Service │──REST──▶│ Game Engine Svc  │
│  (Vite/TS)  │◀──WS────│     (port 8082)       │         │   (port 8081)    │
└─────────────┘         └──────────────────────┘         └──────────────────┘
```

- **Game Engine Service** — authoritative source of truth for board state. Validates moves, detects wins/draws, rejects moves on completed games.
- **Game Session Service** — orchestrates sessions. Creates games on the engine, runs automated simulations via a rule-based strategy, hosts player-vs-computer interactive games, publishes real-time updates over STOMP/WebSocket.
- **UI** — React/TypeScript single-page app. Shows a 3×3 board, status, move history. Supports both automated simulation and interactive play against the computer.

## Modules

| Module | Type | Default Port |
| --- | --- | --- |
| `tictac-common` | Shared Java library (DTOs, enums, validation) | — |
| `game-engine-service` | Spring Boot service | `8081` |
| `game-session-service` | Spring Boot service | `8082` |
| `ui` | React TypeScript frontend (Vite) | `3000` |

## Prerequisites

- **Java 21** (project uses Gradle toolchain, will download JDK if needed)
- **Node.js 18+** (see `ui/.nvmrc`)
- **Gradle** — the included wrapper (`./gradlew`) is sufficient

## Quick Start (Local)

Open three terminals from the project root:

**Terminal 1 — Game Engine:**
```bash
./gradlew :game-engine-service:bootRun
```

**Terminal 2 — Game Session Service:**
```bash
./gradlew :game-session-service:bootRun
```

**Terminal 3 — UI:**
```bash
cd ui
nvm use 18       # or ensure Node 18+ is active
npm ci
npm run dev
```

Open `http://localhost:3000` in a browser.

- Click **"Start Simulation"** to watch an automated game play out in real time.
- Click **"Play against Computer"** to play interactively against the AI.

## Building

### Build Everything (backend + frontend)

```bash
./gradlew clean build
```

This compiles Java sources, runs all backend tests, and builds the UI production bundle.

### Build Only Backend

```bash
./gradlew clean build -x :ui:npmBuild -x :ui:npmInstall
```

### Build Only Frontend

```bash
cd ui
npm ci
npm run build
```

## Running Tests

### All Backend Tests

```bash
./gradlew test
```

### Only Game Engine Tests

```bash
./gradlew :game-engine-service:test
```

### Only Game Session Tests

```bash
./gradlew :game-session-service:test
```

## Health Checks

Verify each service started correctly:

```text
GET http://localhost:8081/health
GET http://localhost:8082/health
```

## API Reference

### Game Engine Service (port 8081)

| Method | Path | Description |
| --- | --- | --- |
| POST | `/games` | Create a new game (empty board) |
| GET | `/games/{gameId}` | Retrieve current game state |
| POST | `/games/{gameId}/move` | Apply a move to the game |

#### POST /games/{gameId}/move

Request body:
```json
{ "player": "X", "position": 4 }
```

Position is 0–8, mapped left-to-right, top-to-bottom. Returns the updated game state.

### Game Session Service (port 8082)

| Method | Path | Description |
| --- | --- | --- |
| POST | `/sessions` | Create a new simulation session |
| POST | `/sessions/{sessionId}/simulate` | Run automated simulation to completion |
| GET | `/sessions/{sessionId}` | Get session state and move history |
| POST | `/sessions/player-vs-computer` | Create a human-vs-computer session |
| POST | `/sessions/{sessionId}/human-move` | Submit a human player move |

#### POST /sessions/player-vs-computer

Request body:
```json
{ "human-player": "X" }
```

Creates a session where the human plays as X (first) or O (second). If the human chose O, the computer makes the opening move immediately.

#### POST /sessions/{sessionId}/human-move

Request body:
```json
{ "position": 4 }
```

Returns the updated session including the human's move and (if the game isn't over) the computer's automatic response.

## Example curl Commands

```bash
# Create a simulation session
curl -s -X POST http://localhost:8082/sessions | jq .

# Run the simulation
curl -s -X POST http://localhost:8082/sessions/{sessionId}/simulate | jq .

# Get session details
curl -s http://localhost:8082/sessions/{sessionId} | jq .

# Get game state from the engine
curl -s http://localhost:8081/games/{gameId} | jq .

# Create a player-vs-computer session (human as X)
curl -s -X POST http://localhost:8082/sessions/player-vs-computer \
  -H 'Content-Type: application/json' \
  -d '{"human-player":"X"}' | jq .

# Submit a human move (position 4 = center)
curl -s -X POST http://localhost:8082/sessions/{sessionId}/human-move \
  -H 'Content-Type: application/json' \
  -d '{"position":4}' | jq .
```

## WebSocket (Real-Time Updates)

The Game Session Service publishes STOMP events as a simulation runs.

- **WebSocket URL:** `ws://localhost:8082/ws` (native) or `http://localhost:8082/ws` (SockJS fallback)
- **Topic:** `/topic/sessions/{sessionId}`

Event payload (after each move and at completion/failure):

```json
{
  "session-id": "...",
  "game-id": "...",
  "session-status": "SIMULATING | IN_PROGRESS | COMPLETED | FAILED",
  "current-game-state": { "game-id": "...", "board": [...], "status": "...", "winner": null, "next-player": "..." },
  "latest-move": { "move-number": 1, "player": "X", "position": 4, "resulting-status": "IN_PROGRESS", ... },
  "move-history": [ ... ],
  "failure-reason": null
}
```

The React UI subscribes to this topic immediately after creating a session and updates the board/status live.

## UI Behavior

- **Start Simulation** — creates a session, subscribes to WebSocket, triggers simulation. Moves appear on the board in real time (500 ms delay between moves).
- **Play against Computer** — prompts for side choice (X or O), creates an interactive session. Human clicks empty cells; the computer responds using the same rule-based `SimulationStrategy` that powers the automated simulation.
- The board, game status, winner/draw, and full move history are displayed.
- API errors are shown inline with a dismiss button.

## Frontend Configuration

The Vite dev server proxies `/sessions` and `/ws` to `http://localhost:8082` (configured in `vite.config.ts`). No CORS configuration is needed during local development.

For production builds the environment variable `VITE_SESSION_API_BASE_URL` can point the UI directly at the session service (see `ui/.env.production`).

## Error Handling

- The Game Engine returns 400 for invalid moves (occupied cell, out-of-turn, invalid position) and 404 for unknown games.
- The Session Service maps engine errors to 502 BAD_GATEWAY, validates request bodies (400), detects missing sessions (404), prevents concurrent simulations (409 CONFLICT), and rejects moves on completed/failed sessions (409).
- The UI displays error messages inline and allows dismissal.

## Testing Summary

| Layer | Scope | Count |
| --- | --- | --- |
| Game Engine unit tests | Service, repository, controller | ~37 |
| Session Service unit tests | Service, strategy, repository, client, controller, event publisher | ~53 |
| Session Integration tests (WireMock) | Full HTTP stack with mocked engine (simulation, PvC, errors, concurrency) | ~37 |
| **Total** | | **~127** |

Tests use JUnit 5, Mockito, AssertJ, Spring MockMvc, WireMock, and `TestRestTemplate` for full-stack integration tests.

## Known Limitations

- **In-memory storage only** — all game and session state is lost on restart. No database is used.
- **No service discovery or API gateway** — services communicate via hard-coded `localhost` URLs.
- **Single-instance** — no horizontal scaling or distributed locking beyond per-session `ReentrantLock`.
- **Computer strategy is deterministic** — the `SimulationStrategy` follows fixed rules (win > block > center > corner > side) and will always make the same moves given the same board.
- **No authentication/authorization** — all endpoints are open.
- **Retry logic** — the Session Service retries failed engine calls (3 attempts, exponential backoff) but does not handle prolonged outages beyond that.

## Possible Future Improvements

- Add persistent storage (e.g., PostgreSQL/Redis) for game and session state
- Introduce service discovery (Eureka/Consul) or an API gateway (Spring Cloud Gateway)
- Add Server-Sent Events or long-polling as a WebSocket alternative
- Implement a smarter computer strategy (minimax with alpha-beta pruning)
- Add player authentication and session ownership
- Support multiplayer (human vs human) with turn-based WebSocket notifications