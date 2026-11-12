package quorma.backend.pack;

import java.util.Map;

/**
 * One kind of pack: what it costs and exactly how many cards it holds. {@code slots} maps a Tier to its number of
 * creature cards (a missing Tier has none). {@code supportSlots} is the number of extra cards that are a spell or an
 * equipment (no Tier). The pack size is the sum of both. {@code featured} marks the type the shop should highlight
 * (a hint for the frontend, no effect on the purchase).
 */
public record PackType(long price, Map<Tier, Integer> slots, int supportSlots, boolean featured) {

    public PackType {
        // TODO(pack-type-validate): price > 0 (the ledger rejects amount 0), no negative slot count (also supportSlots), at
        // least one slot overall. Throw IllegalArgumentException so a bad config stops the app at startup. Copy the map.
    }

    public int size() {
        return slots.values().stream().mapToInt(Integer::intValue).sum() + supportSlots;
    }
}
