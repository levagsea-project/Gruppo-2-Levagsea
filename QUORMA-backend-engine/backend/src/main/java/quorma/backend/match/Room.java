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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

/**
 * A room opened by a player who waits online for an opponent. The creator's deck snapshots are locked here
 * ({@code decks}: an array of {@code {faction, pool_snapshot_id, deck}}); when someone joins, they become the
 * CHALLENGER decks of a challenge that is created already ACCEPTED.
 */
@Entity
@Table(name = "rooms")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** {@code rnd-XXXX-XXXX}. */
    private String code;

    private UUID creatorId;

    /** The faction the creator plays in game 1. */
    private String creatorFaction;

    @JdbcTypeCode(SqlTypes.JSON)
    private String decks;

    @Enumerated(EnumType.STRING)
    @Setter
    private RoomStatus status;

    @Setter
    private UUID challengeId;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    private Instant expiresAt;

    @Setter
    private Instant resolvedAt;

    @Version
    private Long version;

    public Room(String code, UUID creatorId, String creatorFaction, String decks, Instant expiresAt) {
        this.code = code;
        this.creatorId = creatorId;
        this.creatorFaction = creatorFaction;
        this.decks = decks;
        this.status = RoomStatus.OPEN;
        this.expiresAt = expiresAt;
    }
}
