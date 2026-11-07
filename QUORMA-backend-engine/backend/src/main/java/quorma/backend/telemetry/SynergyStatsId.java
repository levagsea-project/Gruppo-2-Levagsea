package quorma.backend.telemetry;

import jakarta.persistence.Embeddable;

@Embeddable
public record SynergyStatsId(String rulesetVersion, String synergyId) {
}
