package quorma.backend.match;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A deck snapshot locked by a challenge, one per side and faction. side is CHALLENGER or OPPONENT. */
@Entity
@Table(name = "challenge_decks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChallengeDeck {

    @EmbeddedId
    private ChallengeDeckId id;

    private UUID poolSnapshotId;

    @JdbcTypeCode(SqlTypes.JSON)
    private String deck;

    public ChallengeDeck(ChallengeDeckId id, UUID poolSnapshotId, String deck) {
        this.id = id;
        this.poolSnapshotId = poolSnapshotId;
        this.deck = deck;
    }
}
