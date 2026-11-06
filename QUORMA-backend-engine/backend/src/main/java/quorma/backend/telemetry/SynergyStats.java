package quorma.backend.telemetry;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Synergy uptime and decisiveness (data/balance.json synergy_health). */
@Entity
@Table(name = "synergy_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SynergyStats {

    @EmbeddedId
    private SynergyStatsId id;

    @Setter
    private long turnsTotal;

    @Setter
    private long turnsActive;

    @Setter
    private long decisiveTurns;

    @Setter
    private Instant updatedAt;

    @Version
    private Long version;

    public SynergyStats(SynergyStatsId id) {
        this.id = id;
    }
}
