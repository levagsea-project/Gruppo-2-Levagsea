package quorma.backend.telemetry;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** Marks a match whose log was already aggregated. */
@Entity
@Table(name = "telemetry_processed_matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TelemetryProcessedMatch {

    @Id
    private UUID matchId;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant processedAt;

    public TelemetryProcessedMatch(UUID matchId) {
        this.matchId = matchId;
    }
}
