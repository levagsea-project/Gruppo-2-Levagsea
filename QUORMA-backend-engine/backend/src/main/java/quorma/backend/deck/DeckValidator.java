package quorma.backend.deck;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;

/**
 * Structural deck rules. Ownership, faction and snapshot membership are checked by {@link DeckService}.
 */
@Component
public class DeckValidator {

    public static final int DECK_SIZE = 32;

    /**
     * Returns one error per broken rule, all of them at once; an empty list means the deck is valid.
     * Rules: exactly {@value #DECK_SIZE} cards; Resistenza Umana 20-23 creatures, Potenze Artificiali 24-27; no two
     * cards share a {@link DeckCardResponse#deckSlot()} (one creature per suit and rank, one copy of each spell and
     * equipment).
     */
    public List<DeckError> validate(Faction faction, List<DeckCardResponse> cards) {
        List<DeckError> errors = new ArrayList<>();

        if (cards.size() != DECK_SIZE) {
            errors.add(DeckError.of(ErrorCodes.DECK_SIZE, "expected", DECK_SIZE, "found", cards.size()));
        }

        int min = switch (faction) {
            case RESISTENZA_UMANA -> 20;
            case POTENZE_ARTIFICIALI -> 24;
        };
        int max = min + 3;
        long creatures = cards.stream().filter(c -> "CREATURE".equals(c.kind())).count();
        if (creatures < min || creatures > max) {
            errors.add(DeckError.of(ErrorCodes.DECK_CREATURE_COUNT, "faction", faction.name(), "min", min,
                    "max", max, "found", creatures));
        }

        Set<String> seenSlots = new HashSet<>();
        Set<String> reported = new HashSet<>();
        for (DeckCardResponse card : cards) {
            if (!seenSlots.add(card.deckSlot()) && reported.add(card.deckSlot())) {
                errors.add(DeckError.of(ErrorCodes.DECK_DUPLICATE_SLOT, "slot", card.deckSlot()));
            }
        }
        return errors;
    }
}
