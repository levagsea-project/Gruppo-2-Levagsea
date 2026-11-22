package quorma.backend.deck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import quorma.backend.catalogue.CardDefinition;
import quorma.backend.catalogue.CardDefinitionRepository;
import quorma.backend.catalogue.CardRepository;
import quorma.backend.catalogue.CardPresenter;
import quorma.backend.catalogue.PreferredLocales;
import quorma.backend.catalogue.PoolSnapshotRepository;
import quorma.backend.collection.CardInstance;
import quorma.backend.collection.CardInstanceRepository;
import quorma.backend.common.ErrorCodes;

/**
 * Joins a deck's card instances with their cards and their definitions in one snapshot. Shared by {@link DeckService}
 * (saving a deck) and {@link DeckMigrator} (moving decks to the active snapshot).
 */
@Component
class DeckLoader {

    private final CardInstanceRepository instances;
    private final CardRepository cards;
    private final CardDefinitionRepository definitions;
    private final PoolSnapshotRepository snapshots;
    private final CardPresenter presenter;

    DeckLoader(CardInstanceRepository instances, CardRepository cards, CardDefinitionRepository definitions,
               PoolSnapshotRepository snapshots, CardPresenter presenter) {
        this.instances = instances;
        this.cards = cards;
        this.definitions = definitions;
        this.snapshots = snapshots;
        this.presenter = presenter;
    }

    /**
     * Joins instances, cards and snapshot definitions into views, adding a message to {@code errors} per problem:
     * repeated instance id, unknown snapshot or snapshot of another faction, instance missing or not owned (one
     * message for both, so other players' ids cannot be probed), card of another faction, card absent from the
     * snapshot. {@code locales} is the language of the text fields, Italian as the fallback; null skips the text (a
     * migration needs only the rules, so it does not pay for the extra queries).
     */
    List<DeckCardResponse> load(UUID ownerId, String faction, UUID snapshotId, List<UUID> instanceIds,
                                List<DeckError> errors, PreferredLocales locales) {
        // repeated instance ids (order of the request is kept)
        Set<UUID> unique = new LinkedHashSet<>();
        Set<UUID> reported = new HashSet<>();
        for (UUID id : instanceIds) {
            if (!unique.add(id) && reported.add(id)) {
                errors.add(DeckError.of(ErrorCodes.DECK_INSTANCE_DUPLICATED, "id", id));
            }
        }

        // snapshot
        boolean snapshotOk = snapshots.findById(snapshotId)
                .filter(s -> Objects.equals(s.getFaction(), faction))
                .isPresent();
        if (!snapshotOk) {
            errors.add(DeckError.of(ErrorCodes.DECK_SNAPSHOT_INVALID));
        }

        // instances: missing or not owned give the same message
        Map<UUID, CardInstance> instMap = instances.findAllById(unique).stream()
                .filter(i -> Objects.equals(i.getOwnerId(), ownerId))
                .collect(Collectors.toMap(i -> i.getId(), i -> i));
        for (UUID id : unique) {
            if (!instMap.containsKey(id)) {
                errors.add(DeckError.of(ErrorCodes.DECK_INSTANCE_NOT_FOUND, "id", id));
            }
        }

        // cards
        Set<UUID> cardIds = instMap.values().stream().map(i -> i.getCardId()).collect(Collectors.toSet());
        var cardMap = cards.findAllById(cardIds).stream().collect(Collectors.toMap(c -> c.getId(), c -> c));

        // definitions in the snapshot
        Map<UUID, CardDefinition> defMap = new HashMap<>();
        if (snapshotOk && !cardIds.isEmpty()) {
            definitions.findBySnapshotIdAndCardIdIn(snapshotId, cardIds)
                    .forEach(d -> defMap.put(d.getCardId(), d));
        }

        // name, flavour and sets: card text and commercial data, outside the snapshot
        Map<UUID, CardPresenter.CardFace> faces = locales == null ? Map.of() : presenter.faces(cardIds, locales);

        List<DeckCardResponse> result = new ArrayList<>();
        for (UUID id : unique) {
            CardInstance inst = instMap.get(id);
            if (inst == null) {
                continue;
            }
            var card = cardMap.get(inst.getCardId());
            if (card == null) {
                continue;
            }
            if (!Objects.equals(card.getFaction(), faction)) {
                errors.add(DeckError.of(ErrorCodes.DECK_CARD_WRONG_FACTION, "card", card.getCardCode()));
                continue;
            }
            CardDefinition def = defMap.get(card.getId());
            if (def == null) {
                if (snapshotOk) {
                    errors.add(DeckError.of(ErrorCodes.DECK_CARD_NO_DEFINITION, "card", card.getCardCode()));
                }
                continue;
            }
            CardPresenter.CardFace face = faces.get(card.getId());
            result.add(new DeckCardResponse(inst.getId(), card.getId(), card.getCardCode(), card.getKind().name(),
                    face == null ? null : face.name(), face == null ? null : face.flavour(),
                    face == null ? null : face.textLocale(), face == null ? List.of() : face.sets(),
                    card.getSubfaction() == null ? null : card.getSubfaction().name(),
                    card.getSuit(), card.getRank(), def.getAtk(), def.getDef(), def.getAbilities(), card.getDeckSlot()));
        }
        return result;
    }

    /** Rows are built from the views, never by hand: the database checks that they are consistent with the deck. */
    List<DeckCard> toRows(Deck deck, List<DeckCardResponse> views) {
        return views.stream()
                .map(v -> new DeckCard(new DeckCardId(deck.getId(), v.cardInstanceId()), deck.getOwnerId(), v.cardId(),
                        deck.getFaction(), deck.getSnapshotId(), v.deckSlot()))
                .toList();
    }
}
