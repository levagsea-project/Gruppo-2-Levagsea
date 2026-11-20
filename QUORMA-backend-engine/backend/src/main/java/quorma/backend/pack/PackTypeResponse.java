package quorma.backend.pack;

import java.util.Map;
import quorma.backend.catalogue.CardPresenter;

/** A pack type on sale, as shown to the player: {@code set} is the set it draws from, named in the language asked for. */
public record PackTypeResponse(String code, long price, int size, Map<Tier, Integer> slots,
                               int supportSlots, boolean featured, CardPresenter.SetRef set) {
}
