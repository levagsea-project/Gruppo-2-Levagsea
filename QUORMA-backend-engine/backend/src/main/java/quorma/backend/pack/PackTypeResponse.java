package quorma.backend.pack;

import java.util.Map;

/** A pack type on sale, as shown to the player. */
public record PackTypeResponse(String code, long price, int size, Map<Tier, Integer> slots,
                               int supportSlots, boolean featured) {
}
