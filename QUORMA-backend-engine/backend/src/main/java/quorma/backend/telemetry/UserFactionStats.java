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

/** Games played and won by a player with one faction (the constant name). */
@Entity
@Table(name = "user_faction_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserFactionStats {

    @EmbeddedId
    private UserFactionStatsId id;

    @Setter
    private long gamesPlayed;

    @Setter
    private long gamesWon;

    @Setter
    private Instant updatedAt;

    @Version
    private Long version;

    public UserFactionStats(UserFactionStatsId id) {
        this.id = id;
    }
}
