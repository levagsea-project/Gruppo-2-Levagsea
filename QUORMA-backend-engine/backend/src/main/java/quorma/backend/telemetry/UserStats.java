package quorma.backend.telemetry;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Aggregated per-user figures. */
@Entity
@Table(name = "user_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserStats {

    @Id
    private UUID userId;

    @Setter
    private long matchesPlayed;

    @Setter
    private long wins;

    @Setter
    private long losses;

    @Setter
    private long draws;

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

    public UserStats(UUID userId) {
        this.userId = userId;
    }
}
