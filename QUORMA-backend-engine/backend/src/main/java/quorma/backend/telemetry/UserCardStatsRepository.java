package quorma.backend.telemetry;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCardStatsRepository extends JpaRepository<UserCardStats, UserCardStatsId> {
}
