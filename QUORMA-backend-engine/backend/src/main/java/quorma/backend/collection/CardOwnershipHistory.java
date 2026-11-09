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
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** Append-only log of ownership changes. */
@Entity
@Table(name = "card_ownership_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardOwnershipHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID cardInstanceId;

    private UUID fromUserId;

    private UUID toUserId;

    @Enumerated(EnumType.STRING)
    private OwnershipReason reason;

    private UUID tradeId;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    public CardOwnershipHistory(UUID cardInstanceId, UUID fromUserId, UUID toUserId, OwnershipReason reason, UUID tradeId) {
        this.cardInstanceId = cardInstanceId;
        this.fromUserId = fromUserId;
        this.toUserId = toUserId;
        this.reason = reason;
        this.tradeId = tradeId;
    }
}
