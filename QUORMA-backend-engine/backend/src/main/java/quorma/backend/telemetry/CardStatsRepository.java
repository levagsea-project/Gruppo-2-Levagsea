package quorma.backend.telemetry;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardStatsRepository extends JpaRepository<CardStats, CardStatsId> {
}
