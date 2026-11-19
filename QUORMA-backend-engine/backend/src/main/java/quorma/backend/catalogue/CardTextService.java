package quorma.backend.catalogue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks up card text (name, flavour) for the languages a caller prefers. The text of a card comes in the first of
 * {@link PreferredLocales#candidates()} that it has, and Italian is the last candidate, so a card always has a name:
 * the database requires its Italian text.
 */
@Service
public class CardTextService {

    public static final String FALLBACK_LOCALE = "it";

    private final CardTextRepository texts;

    public CardTextService(CardTextRepository texts) {
        this.texts = texts;
    }

    /** The name of each card in the best language it has among {@code locales}. */
    @Transactional(readOnly = true)
    public Map<UUID, String> names(Collection<UUID> cardIds, PreferredLocales locales) {
        Map<UUID, String> names = new HashMap<>();
        resolve(cardIds, locales).forEach((id, text) -> names.put(id, text.getName()));
        return names;
    }

    /**
     * The text row of each card in the best language it has: the first of the preferred languages with a row, else
     * Italian. One query for all the cards.
     */
    @Transactional(readOnly = true)
    public Map<UUID, CardText> resolve(Collection<UUID> cardIds, PreferredLocales locales) {
        Map<UUID, CardText> result = new HashMap<>();
        if (cardIds.isEmpty()) {
            return result;
        }
        List<String> candidates = locales.candidates();
        for (CardText row : texts.findByCardIdInAndLocaleIn(cardIds, candidates)) {
            CardText best = result.get(row.getCardId());
            if (best == null || candidates.indexOf(row.getLocale()) < candidates.indexOf(best.getLocale())) {
                result.put(row.getCardId(), row);
            }
        }
        return result;
    }
}
