# Build context: repo root (needs the parent pom, game-engine and data/)
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build
COPY pom.xml .
COPY game-engine/pom.xml game-engine/pom.xml
COPY engine-service/pom.xml engine-service/pom.xml
COPY backend/pom.xml backend/pom.xml
COPY data data
COPY game-engine/src game-engine/src
COPY engine-service/src engine-service/src
RUN mvn -q -B -pl engine-service -am package -DskipTests
# An image is tagged with the engine version it plays: a replay needs the engine that played the match, so the tag must
# never say anything else. ENGINE_VERSION is the tag the caller wants (make engine-image, docker compose); the build
# refuses it if it is not the version the engine itself reports (EngineVersion, from the pom, the one every MatchLog
# names in its header). The real version is written to /app/ENGINE_VERSION either way.
ARG ENGINE_VERSION=
RUN V=$(sed -n 's/^engine.version=//p' game-engine/target/classes/quorma/gameengine/engine.properties)  && echo "$V" > /build/ENGINE_VERSION  && if [ -n "$ENGINE_VERSION" ] && [ "$ENGINE_VERSION" != "$V" ]; then       echo "ENGINE_VERSION=$ENGINE_VERSION, but this engine is $V: the image tag must be the engine version" >&2; exit 1; fi

FROM eclipse-temurin:25-jre
ARG ENGINE_VERSION=
LABEL org.opencontainers.image.title="quorma-engine" org.opencontainers.image.version="${ENGINE_VERSION}"
WORKDIR /app
COPY --from=builder /build/ENGINE_VERSION /app/ENGINE_VERSION
COPY --from=builder /build/engine-service/target/engine-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENV SERVER_PORT=8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
