package quorma.backend.catalogue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What a card shows besides its rules: its name and flavour in the best language it has, the language that text is in,
 * and the sets it belongs to (with their names in the best language each has). All of it is card text and commercial
 * data, outside the snapshot: it changes nothing a match plays. A fixed number of queries however many cards there are.
 */
@Service
public class CardPresenter {

    /** A set a card belongs to: its code (the identifier) and its name in the best language the set has. */
    public record SetRef(String code, String name) {
    }

    /**
     * @param name       the card's name
     * @param flavour    the card's flavour text, null if it has none in that language
     * @param textLocale the language {@code name} and {@code flavour} are in (the one asked for, or {@code it} as the fallback)
     * @param sets       the sets the card belongs to, by code
     */
    public record CardFace(String name, String flavour, String textLocale, List<SetRef> sets) {
    }

    private final CardTextService cardTexts;
    private final CardSetCardRepository links;
    private final CardSetRepository sets;
    private final CardSetTextRepository setTexts;

    public CardPresenter(CardTextService cardTexts, CardSetCardRepository links, CardSetRepository sets,
                         CardSetTextRepository setTexts) {
        this.cardTexts = cardTexts;
        this.links = links;
        this.sets = sets;
        this.setTexts = setTexts;
    }

    /** Each set as {@link SetRef}: its code and its name in the best language it has (the code if it has no text), by set id. */
    @Transactional(readOnly = true)
    public Map<UUID, SetRef> setRefs(Collection<UUID> setIds, PreferredLocales locales) {
        Map<UUID, SetRef> refs = new HashMap<>();
        if (setIds.isEmpty()) {
            return refs;
        }
        List<String> candidates = locales.candidates();
        Map<UUID, CardSetText> best = new HashMap<>();
        for (CardSetText row : setTexts.findBySetIdIn(setIds)) {
            if (!candidates.contains(row.getLocale())) {
                continue;
            }
            CardSetText current = best.get(row.getSetId());
            if (current == null || candidates.indexOf(row.getLocale()) < candidates.indexOf(current.getLocale())) {
                best.put(row.getSetId(), row);
            }
        }
        for (CardSet set : sets.findAllById(setIds)) {
            CardSetText text = best.get(set.getId());
            refs.put(set.getId(), new SetRef(set.getCode(), text == null ? set.getCode() : text.getName()));
        }
        return refs;
    }

    /** The face of each card, by card id. */
    @Transactional(readOnly = true)
    public Map<UUID, CardFace> faces(Collection<UUID> cardIds, PreferredLocales locales) {
        Map<UUID, CardFace> faces = new HashMap<>();
        if (cardIds.isEmpty()) {
            return faces;
        }
        Map<UUID, CardText> texts = cardTexts.resolve(cardIds, locales);

        Map<UUID, List<UUID>> setIdsOfCard = new HashMap<>();
        Set<UUID> setIds = new HashSet<>();
        for (CardSetCard link : links.findByCardIdIn(cardIds)) {
            setIdsOfCard.computeIfAbsent(link.getCardId(), k -> new ArrayList<>()).add(link.getSetId());
            setIds.add(link.getSetId());
        }
        Map<UUID, SetRef> setRefs = setRefs(setIds, locales);

        for (UUID cardId : cardIds) {
            CardText text = texts.get(cardId);
            List<SetRef> refs = setIdsOfCard.getOrDefault(cardId, List.of()).stream()
                    .map(setRefs::get)
                    .sorted(Comparator.comparing(SetRef::code))
                    .toList();
            faces.put(cardId, new CardFace(text == null ? null : text.getName(), text == null ? null : text.getFlavour(),
                    text == null ? null : text.getLocale(), refs));
        }
        return faces;
    }
}
