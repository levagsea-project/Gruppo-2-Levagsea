package quorma.backend.pack;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * One kind of pack: what it costs and exactly how many cards it holds. {@code slots} maps a Tier to its number of
 * creature cards (a missing Tier has none). {@code supportSlots} is the number of extra cards that are a spell or an
 * equipment (no Tier). The pack size is the sum of both. {@code featured} marks the type the shop should highlight
 * (a hint for the frontend, no effect on the purchase).
 */
public record PackType(long price, Map<Tier, Integer> slots, int supportSlots, boolean featured) {

    /** Checks the configuration: a bad pack type stops the app at startup instead of failing at purchase time. */
    public PackType {
        if (price <= 0) {
            throw new IllegalArgumentException("pack price must be > 0, was " + price);
        }
        if (supportSlots < 0) {
            throw new IllegalArgumentException("pack support-slots must be >= 0, was " + supportSlots);
        }
        EnumMap<Tier, Integer> copy = new EnumMap<>(Tier.class);
        if (slots != null) {
            slots.forEach((tier, count) -> {
                if (tier == null || count == null || count < 0) {
                    throw new IllegalArgumentException("pack slots need a Tier and a count >= 0, got " + tier + "=" + count);
                }
                copy.put(tier, count);
            });
        }
        int total = copy.values().stream().mapToInt(i -> i).sum() + supportSlots;
        if (total < 1) {
            throw new IllegalArgumentException("a pack must hold at least one card");
        }
        slots = Collections.unmodifiableMap(copy);
    }

    public int size() {
        return slots.values().stream().mapToInt(i -> i).sum() + supportSlots;
    }
}
