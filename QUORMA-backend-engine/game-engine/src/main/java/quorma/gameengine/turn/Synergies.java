package quorma.gameengine.turn;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import quorma.gameengine.model.Side;

/**
 * The suit synergy tiers each side has active this turn (ruleset 7.10), fixed at Evaluate like the hand: a tier is
 * active when the player has at least its number of creatures of the suit, unless a higher active tier replaces
 * it. Only sides with an active tier are listed. What the tiers do is {@link SynergyEffects}.
 *
 * @param active tier ids such as {@code VINCOLO_3}, in {@code synergies.json} order
 */
public record Synergies(Map<Side, List<String>> active) {

    /** No tier active for either side. */
    public static final Synergies NONE = new Synergies(Map.of());

    public Synergies {
        Map<Side, List<String>> copy = new EnumMap<>(Side.class);
        Objects.requireNonNull(active, "active").forEach((side, tiers) -> {
            if (!tiers.isEmpty()) {
                copy.put(side, List.copyOf(tiers));
            }
        });
        active = Collections.unmodifiableMap(copy);
    }

    /** The active tiers of {@code side}, in file order. */
    public List<String> tiers(Side side) {
        return active.getOrDefault(side, List.of());
    }

    public boolean isActive(Side side, String tierId) {
        return tiers(side).contains(tierId);
    }
}
