package quorma.backend.deck;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A card instance in a deck. The copied columns are kept consistent by composite foreign keys. */
@Entity
@Table(name = "deck_cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeckCard {

    @EmbeddedId
    private DeckCardId id;

    private UUID ownerId;

    private UUID cardId;

    private String faction;

    private UUID snapshotId;

    private String deckSlot;

    public DeckCard(DeckCardId id, UUID ownerId, UUID cardId, String faction, UUID snapshotId, String deckSlot) {
        this.id = id;
        this.ownerId = ownerId;
        this.cardId = cardId;
        this.faction = faction;
        this.snapshotId = snapshotId;
        this.deckSlot = deckSlot;
    }
}
