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

/** Turns a player had each hand type (the engine's constant name). */
@Entity
@Table(name = "user_hand_stats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserHandStats {

    @EmbeddedId
    private UserHandStatsId id;

    @Setter
    private long turns;

    @Setter
    private Instant updatedAt;

    @Version
    private Long version;

    public UserHandStats(UserHandStatsId id) {
        this.id = id;
    }
}
