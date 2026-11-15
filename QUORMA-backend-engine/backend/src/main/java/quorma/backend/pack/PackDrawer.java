package quorma.backend.pack;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import quorma.backend.catalogue.CardKind;

/**
 * Chooses which cards a pack contains. No database access and no credits: only the draw.
 * Packs are not replayed like matches, this does not use the engine's seeded PRNG.
 */
@Component
public class PackDrawer {

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
        // TODO(pack-drawer)
        throw new UnsupportedOperationException("PackDrawer.draw");
    }
}
