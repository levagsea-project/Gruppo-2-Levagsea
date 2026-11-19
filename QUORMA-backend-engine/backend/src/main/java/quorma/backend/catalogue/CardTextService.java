package quorma.backend.catalogue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks up card text (name, flavour) for a locale. Italian is the fallback for a missing locale or a missing row, so a
 * card always has a name: the database requires its Italian text.
 */
@Service
public class CardTextService {

    public static final String FALLBACK_LOCALE = "it";

    private final CardTextRepository texts;

    public CardTextService(CardTextRepository texts) {
        this.texts = texts;
    }

    /** The name of each card in {@code locale}, or in Italian where that locale has none. */
    @Transactional(readOnly = true)
    public Map<UUID, String> names(Collection<UUID> cardIds, String locale) {
        Map<UUID, String> names = new HashMap<>();
        for (Map.Entry<UUID, CardText> e : resolve(cardIds, locale).entrySet()) {
            names.put(e.getKey(), e.getValue().getName());
        }
        return names;
    }

    /** The text row of each card in {@code locale}, or the Italian row where that locale has none. */
    @Transactional(readOnly = true)
    public Map<UUID, CardText> resolve(Collection<UUID> cardIds, String locale) {
        Map<UUID, CardText> result = new HashMap<>();
        if (cardIds.isEmpty()) {
            return result;
        }
        String wanted = locale == null || locale.isBlank() ? FALLBACK_LOCALE : locale;
        List<CardText> rows = texts.findByCardIdInAndLocaleIn(cardIds, List.of(wanted, FALLBACK_LOCALE));
        for (CardText row : rows) {
            if (row.getLocale().equals(wanted) || !result.containsKey(row.getCardId())) {
                result.put(row.getCardId(), row);
            }
        }
        return result;
    }
}
