package quorma.backend.collection;

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
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** One physical copy of a card. Only ownerId and acquiredAt may change. */
@Entity
@Table(name = "card_instances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    private UUID ownerId;

    private UUID cardId;

    @Enumerated(EnumType.STRING)
    private CardOrigin originType;

    private UUID originRef;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @Setter
    private Instant acquiredAt;

    public CardInstance(UUID ownerId, UUID cardId, CardOrigin originType, UUID originRef, Instant acquiredAt) {
        this.ownerId = ownerId;
        this.cardId = cardId;
        this.originType = originType;
        this.originRef = originRef;
        this.acquiredAt = acquiredAt;
    }
}
