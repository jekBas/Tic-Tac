# AGENTS.md

## Project Overview

Distributed Tic-tac-toe — a Gradle multi-module Spring Boot project with a React frontend. Two backend services communicate via REST; the frontend receives real-time updates over WebSocket (STOMP/SockJS).

## Architecture

```
React UI (port 3000) → Game Session Service (port 8082) → Game Engine Service (port 8081)
```

- `tictac-common` — shared library (DTOs, enums, validation annotations)
- `game-engine-service` — authoritative game state, move validation, win/draw detection
- `game-session-service` — session orchestration, AI strategies, retry logic, WebSocket events
- `ui` — React 18 + TypeScript + Vite

## Build & Run

```bash
./gradlew clean build                    # full build (backend + frontend)
./gradlew test                           # all backend tests
./gradlew :game-engine-service:bootRun   # start engine on :8081
./gradlew :game-session-service:bootRun  # start session service on :8082
cd ui && npm ci && npm run dev           # start UI on :3000
```

## Code Conventions

- **Java 21** — use modern language features (records, sealed classes, pattern matching where applicable)
- **Package structure:** `com.example.tictac.engine.*`, `com.example.tictac.session.*`, `com.example.tictac.common.*`
- **Naming:** classes PascalCase, methods/fields camelCase, constants UPPER_SNAKE_CASE
- **Jackson property naming:** kebab-case for JSON serialization (configured via `spring.jackson.property-naming-strategy=KebabCaseStrategy`)
- **No Lombok** — this project does not use Lombok; write constructors/getters explicitly or use records
- **Tabs for indentation** in Java source files
- **Validation:** use Jakarta Bean Validation annotations (`@NotNull`, custom `@PositionConstraint`)
- **Error handling:** `@RestControllerAdvice` per service with structured error responses
- **Logging:** SLF4J via `LoggerFactory.getLogger(ClassName.class)`

## Commit Messages

Follow conventional commits format:

```
feat(scope): short description
fix(scope): short description
test(scope): short description
docs: short description
```

Scopes: `game-engine`, `game-session`, `ui`, `common`

## Testing

- **Unit tests:** JUnit 5 + Mockito + AssertJ
- **Integration tests:** `@SpringBootTest` + `TestRestTemplate` + WireMock for mocking the Game Engine
- **Async assertions:** Awaitility for WebSocket/async behavior
- **Test isolation:** `SessionRepository.clear()` in `@BeforeEach`; WireMock resets between tests
- **Test config:** `application-integration-test.properties` with dynamic WireMock port injection
- **Parametrized tests:** use `@ParameterizedTest` with `ArgumentsProvider` for move validation scenarios

When adding new features, write tests at both unit and integration levels.

## Key Patterns

- **Concurrency:** `ReentrantLock` per game/session, `tryLock()` for non-blocking conflict detection (returns 409)
- **Retry:** `@Retryable` on `GameEngineClient` (3 attempts, 500ms backoff, 2x multiplier)
- **Strategy pattern:** `SimulationStrategy` (rule-based) and `RandomMoveStrategy` implement move selection
- **Event publishing:** `SimpMessagingTemplate` pushes to `/topic/sessions/{id}` after each move
- **In-memory storage:** `ConcurrentHashMap`-based repositories — no database
- **Thread-safe collections:** `CopyOnWriteArrayList` for move history

## Frontend Conventions

- TypeScript strict mode
- Components in `src/components/`, API layer in `src/api/`, types in `src/types/`
- Vite dev server proxies `/sessions` and `/ws` to `localhost:8082`
- No CSS framework — plain CSS in `index.css`

## Things to Avoid

- Do not add a database without adding Flyway migrations
- Do not bypass `@Retryable` — always let retry logic handle transient engine failures
- Do not use `synchronized` blocks — use `ReentrantLock` for consistency with existing code
- Do not introduce Lombok
- Do not change JSON property naming strategy (kebab-case is used across all services and frontend)