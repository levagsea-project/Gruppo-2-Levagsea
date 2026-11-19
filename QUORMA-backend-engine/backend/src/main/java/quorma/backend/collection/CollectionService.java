package quorma.backend.collection;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.ActiveSnapshotService;
import quorma.backend.catalogue.Card;
import quorma.backend.catalogue.CardDefinition;
import quorma.backend.catalogue.CardDefinitionRepository;
import quorma.backend.catalogue.CardRepository;
import quorma.backend.catalogue.CardPresenter;
import quorma.backend.catalogue.PreferredLocales;
import quorma.backend.catalogue.Faction;

/** Read side of the collection. */
@Service
public class CollectionService {

    private final CardInstanceRepository instances;
    private final CardRepository cards;
    private final CardDefinitionRepository definitions;
    private final ActiveSnapshotService activeSnapshots;
    private final CardPresenter presenter;

    public CollectionService(CardInstanceRepository instances, CardRepository cards,
                             CardDefinitionRepository definitions, ActiveSnapshotService activeSnapshots,
                             CardPresenter presenter) {
        this.instances = instances;
        this.cards = cards;
        this.definitions = definitions;
        this.activeSnapshots = activeSnapshots;
        this.presenter = presenter;
    }

    /** Every instance owned by the user, newest first, with card data from the active snapshot of each faction. */
    @Transactional(readOnly = true)
    public CollectionResponse get(UUID ownerId, PreferredLocales locales) {
        Resolved resolved = resolve(instances.findByOwnerIdOrderByAcquiredAtDesc(ownerId), locales);
        return new CollectionResponse(resolved.snapshotIds(), resolved.views());
    }

    /**
     * Turns instances into views. Loads their cards in one query, then the definitions of those cards in the active
     * snapshot of each faction in one query per faction (no query per instance). Also used by
     * {@code PackService} for the cards of a new pack. A card missing from the latest snapshot (possible when a
     * newer pool drops it) still appears, with its name but null stats and abilities.
     */
    @Transactional(readOnly = true)
    public List<CollectionCardResponse> toViews(List<CardInstance> owned, PreferredLocales locales) {
        return resolve(owned, locales).views();
    }

    /** The views, in the order of {@code owned}, and the snapshots used to build them. */
    private record Resolved(List<UUID> snapshotIds, List<CollectionCardResponse> views) {
    }

    private Resolved resolve(List<CardInstance> owned, PreferredLocales locales) {
        if (owned.isEmpty()) {
            return new Resolved(List.of(), List.of());
        }

        Set<UUID> cardIds = new HashSet<>();
        for (CardInstance instance : owned) {
            cardIds.add(instance.getCardId());
        }
        Map<UUID, Card> cardById = new HashMap<>();
        Map<Faction, Set<UUID>> cardIdsByFaction = new EnumMap<>(Faction.class);
        for (Card card : cards.findAllById(cardIds)) {
            cardById.put(card.getId(), card);
            cardIdsByFaction.computeIfAbsent(Faction.valueOf(card.getFaction()), f -> new HashSet<>())
                    .add(card.getId());
        }

        List<UUID> snapshotIds = new ArrayList<>();
        Map<UUID, CardDefinition> definitionByCardId = new HashMap<>();
        cardIdsByFaction.forEach((faction, ids) ->
                activeSnapshots.activeId(faction.name()).ifPresent(snapshotId -> {
                    snapshotIds.add(snapshotId);
                    for (CardDefinition definition : definitions.findBySnapshotIdAndCardIdIn(snapshotId, ids)) {
                        definitionByCardId.put(definition.getCardId(), definition);
                    }
                }));

        // Name, flavour and sets are card text and commercial data, outside the snapshot: a card has them even when its
        // faction's active snapshot lacks it. They follow the language asked for, Italian as the fallback.
        Map<UUID, CardPresenter.CardFace> faces = presenter.faces(cardIds, locales);

        List<CollectionCardResponse> views = new ArrayList<>(owned.size());
        for (CardInstance instance : owned) {
            Card card = cardById.get(instance.getCardId());
            if (card == null) {
                // card_instances.card_id is a foreign key: this means the data is broken, not a client error.
                throw new IllegalStateException("card " + instance.getCardId() + " of instance " + instance.getId()
                        + " not found");
            }
            views.add(toView(instance, card, faces.get(card.getId()), definitionByCardId.get(card.getId())));
        }
        return new Resolved(List.copyOf(snapshotIds), views);
    }

    /** {@code definition} is null when the card is not in the active snapshot of its faction; the card text ({@code face}) is always present. */
    private static CollectionCardResponse toView(CardInstance instance, Card card, CardPresenter.CardFace face,
                                                 CardDefinition definition) {
        return new CollectionCardResponse(
                instance.getId(),
                card.getId(),
                card.getCardCode(),
                card.getFaction(),
                card.getKind().name(),
                face.name(),
                face.flavour(),
                face.textLocale(),
                face.sets(),
                card.getSubfaction() == null ? null : card.getSubfaction().name(),
                card.getSuit(),
                card.getRank(),
                definition == null ? null : definition.getAtk(),
                definition == null ? null : definition.getDef(),
                definition == null ? null : definition.getAbilities(),
                instance.getOriginType().name(),
                instance.getAcquiredAt());
    }
}
