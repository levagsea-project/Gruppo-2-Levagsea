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

/** Global per-card figures inside one pool snapshot. */
@Entity
@Table(name = "card_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardStats {

    @EmbeddedId
    private CardStatsId id;

    @Setter
    private long matchesPlayed;

    @Setter
    private long matchesWon;

    @Setter
    private long timesDrawn;

    @Setter
    private long damageDealt;

    @Setter
    private long spillDamage;

    @Setter
    private long kills;

    @Setter
    private Instant updatedAt;

    @Version
    private Long version;

    public CardStats(CardStatsId id) {
        this.id = id;
    }
}
