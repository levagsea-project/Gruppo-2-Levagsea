# engine-service/: agent guide

Spring Boot 4.1.x HTTP microservice (Docker container, port 8081) that exposes `game-engine` to the outside world. It only routes requests: no game rules, no simulation logic, nothing that can affect determinism lives here. Rules belong in `game-engine/`.

- Depends on `game-engine` (never the other way round).
- Build context for its Dockerfile is the **repo root**: `docker compose build engine`.
- `GET /engine/info` returns status and engine version.
- `POST /v1/simulate` takes a match input (JSON) and returns the canonical MatchLog JSON, byte for byte what `GameEngine.matchLog` writes. An input the engine refuses (`MatchInputException`) is a 400 `ProblemDetail`; any other exception is a bug and stays a 500. Not exposed to the outside: only the backend calls it.
