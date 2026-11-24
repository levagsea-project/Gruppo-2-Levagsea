package quorma.backend.telemetry;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFactionStatsRepository extends JpaRepository<UserFactionStats, UserFactionStatsId> {
}
