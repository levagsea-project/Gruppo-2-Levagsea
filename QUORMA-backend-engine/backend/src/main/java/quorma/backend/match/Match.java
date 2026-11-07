package quorma.backend.match;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

/** Immutable replay recipe of a played match (not the log). */
@Entity
@Table(name = "matches")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID seriesId;

    private int gameNumber;

    @Column(name = "player_a_id")
    private UUID playerAId;

    @Column(name = "player_b_id")
    private UUID playerBId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deck_a")
    private String deckA;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "deck_b")
    private String deckB;

    @Column(name = "pool_snapshot_a_id")
    private UUID poolSnapshotAId;

    @Column(name = "pool_snapshot_b_id")
    private UUID poolSnapshotBId;

    private long seed;

    private String rulesetVersion;

    private String rulesetHash;

    private String engineVersion;

    private String prngName;

    private String shuffleAlgorithm;

    private String resultHash;

    @Enumerated(EnumType.STRING)
    private MatchOutcome outcome;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant playedAt;

    public Match(UUID seriesId, int gameNumber, UUID playerAId, UUID playerBId, String deckA, String deckB, UUID poolSnapshotAId, UUID poolSnapshotBId, long seed, String rulesetVersion, String rulesetHash, String engineVersion, String prngName, String shuffleAlgorithm, String resultHash, MatchOutcome outcome) {
        this.seriesId = seriesId;
        this.gameNumber = gameNumber;
        this.playerAId = playerAId;
        this.playerBId = playerBId;
        this.deckA = deckA;
        this.deckB = deckB;
        this.poolSnapshotAId = poolSnapshotAId;
        this.poolSnapshotBId = poolSnapshotBId;
        this.seed = seed;
        this.rulesetVersion = rulesetVersion;
        this.rulesetHash = rulesetHash;
        this.engineVersion = engineVersion;
        this.prngName = prngName;
        this.shuffleAlgorithm = shuffleAlgorithm;
        this.resultHash = resultHash;
        this.outcome = outcome;
    }
}
