package quorma.backend.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.postgresql.PostgreSQLContainer;
import quorma.backend.catalogue.publish.CardChecker;
import quorma.gameengine.GameEngine;
import tools.jackson.databind.json.JsonMapper;

/**
 * A real PostgreSQL for the integration tests (one container, shared by every test class that uses this
 * configuration), and the real engine's card check in-process instead of the HTTP call to the engine-service.
 */
@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgres() {
        return new PostgreSQLContainer("postgres:16");
    }

    @Bean
    @Primary
    CardChecker inProcessCardChecker() {
        GameEngine engine = new GameEngine();
        JsonMapper mapper = JsonMapper.builder().build();
        return request -> mapper.readTree(engine.cardCheck(mapper.writeValueAsString(request)));
    }
}
