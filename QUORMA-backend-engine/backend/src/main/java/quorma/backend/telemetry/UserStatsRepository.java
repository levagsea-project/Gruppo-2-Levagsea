package quorma.backend.telemetry;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UserStatsRepository extends JpaRepository<UserStats, UUID> {
}
