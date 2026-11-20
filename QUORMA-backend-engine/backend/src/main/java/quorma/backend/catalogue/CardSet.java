package quorma.backend.catalogue;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.EventType;

/** A group of cards sold together (V2 {@code card_sets}). {@code status} is BOZZA, IN_VENDITA or CHIUSA; mutable, outside the snapshot. */
@Entity
@Table(name = "card_sets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardSet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String code;

    @Setter
    @Enumerated(EnumType.STRING)
    private CardSetStatus status;

    @Setter
    private LocalDate releaseDate;

    @Setter
    private LocalDate closeDate;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public CardSet(String code, CardSetStatus status, LocalDate releaseDate, LocalDate closeDate) {
        this.code = code;
        this.status = status;
        this.releaseDate = releaseDate;
        this.closeDate = closeDate;
    }
}
