# Distributed Tic-tac-toe Microservices

A distributed Tic-tac-toe game built with Spring Boot microservices, real-time WebSocket updates, and a React frontend.

## Architecture

```text
┌─────────────┐         ┌───────────────────────┐          ┌──────────────────┐
│   React UI  │──REST──▶│  Game Session Service  │──REST──▶│ Game Engine Svc  │
│  (Vite/TS)  │◀──WS────│      (port 8082)       │         │   (port 8081)    │
└─────────────┘         └───────────────────────┘          └──────────────────┘
```

| Module | Description | Port |
| --- | --- | --- |
| `tictac-common` | Shared DTOs, enums, validation | — |
| `game-engine-service` | Game state, move validation, win/draw detection | `8081` |
| `game-session-service` | Session orchestration, AI strategies, WebSocket events | `8082` |
| `ui` | React/TypeScript SPA (Vite) | `3000` |

## Key Features

- **Automated simulation** — watch AI vs AI games play out in real time via WebSocket
- **Player vs Computer** — interactive play with two difficulty levels (rule-based smart AI / random)
- **Concurrency-safe** — per-game `ReentrantLock`, `tryLock()` prevents concurrent simulations
- **Resilient communication** — retry with exponential backoff (3 attempts, 500ms base) between services
- **Comprehensive testing** — ~127 tests (unit + integration with WireMock)

## Tech Stack

- Java 21, Spring Boot 3.5, Spring WebSocket, Spring Retry
- React 18, TypeScript, Vite, STOMP/SockJS
- JUnit 5, Mockito, AssertJ, WireMock, Awaitility

## Quick Start (Docker)

```bash
docker compose up --build    # build and start all services
docker compose down          # stop and remove containers
```

Open `http://localhost:3000`.

## Quick Start (Local)

```bash
# Terminal 1 — Game Engine
./gradlew :game-engine-service:bootRun

# Terminal 2 — Session Service
./gradlew :game-session-service:bootRun

# Terminal 3 — UI
cd ui && npm ci && npm run dev
```

Open `http://localhost:3000`.

## Build & Test

```bash
./gradlew clean build    # build everything (backend + frontend)
./gradlew test           # run all backend tests
```

## API Overview

### Game Engine (`localhost:8081`)

| Method | Path | Description |
| --- | --- | --- |
| POST | `/games` | Create a new game |
| GET | `/games/{gameId}` | Get game state |
| POST | `/games/{gameId}/move` | Apply a move (`{"player":"X","position":4}`) |

### Session Service (`localhost:8082`)

| Method | Path | Description |
| --- | --- | --- |
| POST | `/sessions` | Create simulation session |
| POST | `/sessions/{id}/simulate` | Run automated simulation |
| GET | `/sessions/{id}` | Get session state |
| POST | `/sessions/player-vs-computer` | Create interactive session (`{"human-player":"X","difficulty":"SMART"}`) |
| POST | `/sessions/{id}/human-move` | Submit human move (`{"position":4}`) |

### WebSocket

Subscribe to `/topic/sessions/{sessionId}` via STOMP at `ws://localhost:8082/ws` for real-time game updates.

## Design Decisions

- **In-memory storage** — deliberate choice for simplicity; game state is ephemeral by nature in a demo
- **Two separate services** — demonstrates inter-service communication, retry patterns, and failure isolation
- **Rule-based AI** — deterministic strategy (win > block > center > corner > side) keeps behavior predictable and testable
- **WireMock integration tests** — session service tests are fully decoupled from the engine

## Future Improvements

- **OpenAPI / Swagger UI** — auto-generated API documentation from controller annotations
- **CI/CD pipeline** — GitHub Actions for build, test, and coverage reporting
- **Observability** — Spring Boot Actuator + Micrometer metrics (games played, win rate, response times)
- **Persistence** — PostgreSQL + Flyway migrations for game history and leaderboard
- **Code coverage reporting** — Jacoco with minimum threshold enforcement
- **Contract testing** — Spring Cloud Contract or PACT testing between session and engine services
- **Event sourcing** — each move as an immutable event, enabling replay and audit
