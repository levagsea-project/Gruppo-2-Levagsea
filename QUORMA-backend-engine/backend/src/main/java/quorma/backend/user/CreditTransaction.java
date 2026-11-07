package quorma.backend.user;

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

/** One immutable credit movement (ledger row). Never updated or deleted. */
@Entity
@Table(name = "credit_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    private long amount;

    private long balanceAfter;

    @Enumerated(EnumType.STRING)
    private CreditTransactionType type;

    private String referenceType;

    private UUID referenceId;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    public CreditTransaction(UUID userId, long amount, long balanceAfter, CreditTransactionType type, String referenceType, UUID referenceId) {
        this.userId = userId;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.type = type;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
    }
}
