package quorma.backend.deck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        List<String> errors = new ArrayList<>();

        if (cards.size() != DECK_SIZE) {
            errors.add("Deck must contain exactly " + DECK_SIZE + " cards, found " + cards.size());
        }

        int min;
        int max;
        if (RESISTENZA_UMANA.equals(faction)) {
            min = 20;
            max = 23;
        } else if (POTENZE_ARTIFICIALI.equals(faction)) {
            min = 24;
            max = 27;
        } else {
            errors.add("Unknown faction: " + faction);
            min = -1;
            max = -1;
        }
        if (min >= 0) {
            long creatures = cards.stream().filter(c -> "CREATURE".equals(c.kind())).count();
            if (creatures < min || creatures > max) {
                errors.add("Deck must contain " + min + "-" + max + " creatures for " + faction
                        + ", found " + creatures);
            }
        }

        Set<String> seenSlots = new HashSet<>();
        Set<String> reported = new HashSet<>();
        for (DeckCardResponse card : cards) {
            if (!seenSlots.add(card.deckSlot()) && reported.add(card.deckSlot())) {
                errors.add("Duplicate deck slot: " + card.deckSlot());
            }
        }

        return errors;
    }
}
