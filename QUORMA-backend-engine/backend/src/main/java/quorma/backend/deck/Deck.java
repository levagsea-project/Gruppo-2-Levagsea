package quorma.backend.deck;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** A saved selection of owned cards. Cards may be reused across decks. */
@Entity
@Table(name = "decks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Deck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID ownerId;

    @Setter
    private String name;

    private String faction;

    @Setter
    private UUID snapshotId;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @Setter
    private Instant updatedAt;

    @Version
    private Long version;

    public Deck(UUID ownerId, String name, String faction, UUID snapshotId, Instant updatedAt) {
        this.ownerId = ownerId;
        this.name = name;
        this.faction = faction;
        this.snapshotId = snapshotId;
        this.updatedAt = updatedAt;
    }
}
