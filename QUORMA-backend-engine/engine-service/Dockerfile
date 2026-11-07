# Build context: repo root (needs the parent pom and game-engine)
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /build
COPY pom.xml .
COPY game-engine/pom.xml game-engine/pom.xml
COPY engine-service/pom.xml engine-service/pom.xml
COPY backend/pom.xml backend/pom.xml
COPY game-engine/src game-engine/src
COPY engine-service/src engine-service/src
RUN mvn -q -B -pl engine-service -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=builder /build/engine-service/target/engine-service-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENV SERVER_PORT=8081
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
