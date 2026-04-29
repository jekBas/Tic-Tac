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
    └── src/main/
```

## Modules

| Module | Type | Default Port |
| --- | --- | --- |
| `game-engine-service` | Spring Boot backend service | `8081` |
| `game-session-service` | Spring Boot backend service | `8082` |
| `ui` | Frontend/static UI placeholder | N/A |

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

## Health Checks

The backend services include minimal startup verification endpoints:

```text
GET http://localhost:8081/health
GET http://localhost:8082/health
```

These endpoints exist only to verify that each service starts independently.
