# engine-service/: agent guide

Spring Boot 4.1.x HTTP microservice (Docker container, port 8081) that exposes `game-engine` to the outside world. It only routes requests: no game rules, no simulation logic, nothing that can affect determinism lives here. Rules belong in `game-engine/`.

- Depends on `game-engine` (never the other way round).
- Build context for its Dockerfile is the **repo root**: `docker compose build engine`.
- `GET /engine/info` returns status and engine version.
