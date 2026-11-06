package quorma.backend.telemetry;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SynergyStatsRepository extends JpaRepository<SynergyStats, SynergyStatsId> {
}
