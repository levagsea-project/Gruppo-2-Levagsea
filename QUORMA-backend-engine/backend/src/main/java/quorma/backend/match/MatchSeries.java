package quorma.backend.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** A series of one or more games between the same two players. Player A started it. */
@Entity
@Table(name = "match_series")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private SeriesKind kind;

    @Enumerated(EnumType.STRING)
    private SeriesFormat format;

    private UUID challengeId;

    @Column(name = "player_a_id")
    private UUID playerAId;

    @Column(name = "player_b_id")
    private UUID playerBId;

    @Enumerated(EnumType.STRING)
    @Setter
    private SeriesStatus status;

    @Enumerated(EnumType.STRING)
    @Setter
    private MatchOutcome outcome;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @Setter
    private Instant finishedAt;

    @Version
    private Long version;

    public MatchSeries(SeriesKind kind, SeriesFormat format, UUID challengeId, UUID playerAId, UUID playerBId, SeriesStatus status) {
        this.kind = kind;
        this.format = format;
        this.challengeId = challengeId;
        this.playerAId = playerAId;
        this.playerBId = playerBId;
        this.status = status;
    }
}
