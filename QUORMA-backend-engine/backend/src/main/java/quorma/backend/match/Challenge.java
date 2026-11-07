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

/** A challenge to a chosen player, who accepts (choosing decks) or declines. Locked decks are in ChallengeDeck. */
@Entity
@Table(name = "challenges")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Challenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID challengerId;

    private UUID opponentId;

    @Enumerated(EnumType.STRING)
    private SeriesFormat format;

    @Enumerated(EnumType.STRING)
    @Setter
    private ChallengeStatus status;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    private Instant expiresAt;

    @Setter
    private Instant resolvedAt;

    @Version
    private Long version;

    public Challenge(UUID challengerId, UUID opponentId, SeriesFormat format, ChallengeStatus status, Instant expiresAt) {
        this.challengerId = challengerId;
        this.opponentId = opponentId;
        this.format = format;
        this.status = status;
        this.expiresAt = expiresAt;
    }
}
