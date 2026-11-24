package quorma.backend.telemetry;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserHandStatsRepository extends JpaRepository<UserHandStats, UserHandStatsId> {
}
