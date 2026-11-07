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

/** Per-user per-card figures. */
@Entity
@Table(name = "user_card_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCardStats {

    @EmbeddedId
    private UserCardStatsId id;

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

    public UserCardStats(UserCardStatsId id) {
        this.id = id;
    }
}
