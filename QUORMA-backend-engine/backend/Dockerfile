# Build context: repo root (needs the parent pom and game-engine, a test dependency of the backend)
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build
COPY pom.xml .
COPY game-engine/pom.xml game-engine/pom.xml
COPY engine-service/pom.xml engine-service/pom.xml
COPY backend/pom.xml backend/pom.xml
COPY game-engine/src game-engine/src
COPY backend/src backend/src
# -am: the backend has game-engine as a test dependency (tests run the real card check), so the module is built too
RUN mvn -q -B -pl backend -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /build/backend/target/backend-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV SERVER_PORT=8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
