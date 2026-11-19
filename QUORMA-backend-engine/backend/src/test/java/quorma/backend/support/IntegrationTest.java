package quorma.backend.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base of the tests that need the database. Skipped when Docker is not running. The catalogue bootstrap is off (no
 * folder is configured: a test loads the base set itself when it needs it). Every test starts from an empty database: TRUNCATE does not fire the "immutable" row triggers, so it can clear the append-only tables.
 */
@SpringBootTest
@Import(IntegrationTestConfig.class)
@Testcontainers(disabledWithoutDocker = true)
public abstract class IntegrationTest {

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE users, cards, pool_snapshots, card_sets, catalogue_drafts, pack_types RESTART IDENTITY CASCADE");
    }

    protected int count(String table) {
        Integer n = jdbc.queryForObject("select count(*) from " + table, Integer.class);
        return n == null ? 0 : n;
    }
}
