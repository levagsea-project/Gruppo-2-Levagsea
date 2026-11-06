package quorma.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Quorma Game Engine microservice.
 *
 * <p>Spring Boot provides the HTTP server layer on a configured port. The simulation
 * core stays single-threaded and deterministic; this class only boots the server.
 */
@SpringBootApplication
public class EngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(EngineApplication.class, args);
    }
}