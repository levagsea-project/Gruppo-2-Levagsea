package quorma.backend.pack;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import quorma.backend.catalogue.CardKind;

/**
 * Chooses which cards a pack contains. No database access and no credits: only the draw.
 * Packs are not replayed like matches, this does not use the engine's seeded PRNG.
 */
@Component
public class PackDrawer {

    /** Thread-safe, shared by all requests. */
    private final SecureRandom random = new SecureRandom();

    /** A card of the pool. {@code tier} is null for a spell or an equipment (they have no rank). */
    public record PoolCard(UUID cardId, CardKind kind, Tier tier) {
    }

    /**
     * Returns {@code type.size()} card ids: for each Tier in {@code type.slots()}, exactly that many creatures of the
     * Tier, each chosen uniformly from the creatures of that Tier in {@code pool}; then {@code type.supportSlots()}
     * cards chosen uniformly from all spells and equipment in the pool (one list, so a kind with more cards is more
     * likely). Use {@link java.security.SecureRandom} (cards have value, the result must not be predictable).
     * The same card may come out more than once: each one becomes its own instance.
     * Throw {@link IllegalStateException} if a Tier with slots, or the support slots, have no card in the pool.
     */
    public List<UUID> draw(List<PoolCard> pool, PackType type) {
        Map<Tier, List<UUID>> creaturesByTier = new EnumMap<>(Tier.class);
        List<UUID> support = new ArrayList<>();
        for (PoolCard card : pool) {
            if (card.kind() == CardKind.CREATURE) {
                if (card.tier() == null) {
                    throw new IllegalArgumentException("creature " + card.cardId() + " has no Tier");
                }
                creaturesByTier.computeIfAbsent(card.tier(), t -> new ArrayList<>()).add(card.cardId());
            } else {
                support.add(card.cardId());
            }
        }

        List<UUID> drawn = new ArrayList<>(type.size());
        for (Map.Entry<Tier, Integer> slot : type.slots().entrySet()) {
            pick(creaturesByTier.getOrDefault(slot.getKey(), List.of()), slot.getValue(), drawn, slot.getKey().name());
        }
        pick(support, type.supportSlots(), drawn, "support");
        return drawn;
    }

    /** Adds {@code count} cards chosen uniformly (with repetition) from {@code candidates} to {@code drawn}. */
    private void pick(List<UUID> candidates, int count, List<UUID> drawn, String slotName) {
        if (count == 0) {
            return;
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException("the pool has no card for the " + slotName + " slots");
        }
        for (int i = 0; i < count; i++) {
            drawn.add(candidates.get(random.nextInt(candidates.size())));
        }
    }
}
