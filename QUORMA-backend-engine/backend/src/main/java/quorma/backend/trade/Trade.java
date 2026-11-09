package quorma.backend.trade;

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

/** A trade proposal. Accepting it swaps ownership in one transaction. */
@Entity
@Table(name = "trades")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID proposerId;

    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Setter
    private TradeStatus status;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @Setter
    private Instant resolvedAt;

    @Version
    private Long version;

    public Trade(UUID proposerId, UUID recipientId, TradeStatus status) {
        this.proposerId = proposerId;
        this.recipientId = recipientId;
        this.status = status;
    }
}
