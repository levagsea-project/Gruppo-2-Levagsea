package quorma.backend.deck;

import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Structural deck rules. Ownership, faction and snapshot membership are checked by {@link DeckService}.
 */
@Component
public class DeckValidator {

    public static final int DECK_SIZE = 32;
    public static final String RESISTENZA_UMANA = "Resistenza Umana";
    public static final String POTENZE_ARTIFICIALI = "Potenze Artificiali";

    /**
     * Returns one message per broken rule, all of them at once; an empty list means the deck is valid.
     * Rules: exactly {@value #DECK_SIZE} cards; Resistenza Umana 20-23 creatures, Potenze Artificiali 24-27; no two
     * cards share a {@link DeckCardResponse#deckSlot()} (one creature per suit and rank, one copy of each spell and
     * equipment).
     */
    public List<String> validate(String faction, List<DeckCardResponse> cards) {
        // TODO(deck-validator)
        throw new UnsupportedOperationException("DeckValidator.validate");
    }
}
