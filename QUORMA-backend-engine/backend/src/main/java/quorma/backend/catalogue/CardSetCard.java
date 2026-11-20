package quorma.backend.catalogue;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** A card belongs to a set (V2 {@code card_set_cards}); a card may be in several (reprints). Removing the row takes the card out of that set's packs and nothing else. */
@Entity
@Table(name = "card_set_cards")
@IdClass(CardSetCard.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardSetCard {

    public record Key(UUID cardId, UUID setId) implements Serializable {
    }

    @Id
    private UUID cardId;

    @Id
    private UUID setId;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    public CardSetCard(UUID cardId, UUID setId) {
        this.cardId = cardId;
        this.setId = setId;
    }
}
