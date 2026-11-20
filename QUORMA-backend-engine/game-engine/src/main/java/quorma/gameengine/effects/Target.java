package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import quorma.gameengine.data.Value;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/**
 * One target of the vocabulary (S…): who an effect aims at. One class per target ID, found through {@link Targets}.
 * The same ID is used in a few ways (a static effect reaching creatures, a triggered effect choosing when it resolves,
 * a spell or equipment aiming, a player's ability), so a target has one method per way. A class implements only the
 * ways its ID has; the others keep the default, which says the ID does not work there. Targets that are not
 * creatures (a lane, a player, the deck) have no way here: the turn code acts on them by ID.
 */
public interface Target {

    /** The vocabulary ID this class implements, such as {@code "S9"}. Never renumbered. */
    String id();

    /** True if the effect has one creature to aim at, so with none left it fizzles as "target destroyed". */
    default boolean single() {
        return false;
    }

    /**
     * True if the target params, as the card writes them, are ones the engine reads for this target. The default takes
     * {@code filters} and {@code from} only.
     */
    default boolean acceptsParams(Map<String, Value> params) {
        return TargetParams.accepts(params, Map.of());
    }

    /**
     * Whether a static effect ({@code T0}) of the ability's side reaches {@code receiver}, a creature of {@code side}.
     * {@code source} is the creature whose ability it is, null for a spell. Filters are applied by the caller.
     */
    default boolean reaches(TargetView view, Side abilitySide, Creature source, Side side, Creature receiver) {
        return false;
    }

    /**
     * The living creatures a triggered effect of {@code source} (a creature of {@code side}) reaches when it
     * resolves.
     */
    default List<Creature> select(TargetView view, Side side, Creature source, TriggerContext ctx, BoundExpansion e) {
        throw new IllegalStateException("target " + id() + " has no selection for a triggered effect");
    }

    /**
     * The creatures an immediate effect of a spell or equipment of {@code owner} aims at. {@code receiver} is the
     * creature an equipment was applied to; {@code chosen} holds what selection targets picked so far, by target ID,
     * and gets this one's pick. The default is a pick by ranking, which fails for IDs that are not ranked targets.
     */
    default List<Creature> selectImmediate(TargetView view, BoundExpansion e, Side owner, Creature receiver,
            Map<String, Creature> chosen) {
        Creature picked = view.select(id(), e.targetParams(), owner).orElse(null);
        List<Creature> out = new ArrayList<>();
        if (picked != null) {
            chosen.putIfAbsent(id(), picked);
            out.add(picked);
        }
        return out;
    }

    /**
     * The living creatures an ability of a player ({@code source} null) or of {@code source} that fires at the start of
     * Refill is aimed at. The default is none.
     */
    default List<Creature> ofPlayerAbility(TargetView view, Side side, Creature source, BoundExpansion e) {
        return List.of();
    }

    /**
     * The one creature a hand effect (E63) aims at, seen from {@code side}; {@code source} is null for a spell's aura.
     * The default is a pick by ranking, which fails for IDs that are not ranked targets.
     */
    default Optional<Creature> pick(TargetView view, BoundExpansion e, Side side, Creature source) {
        return view.select(id(), e.targetParams(), side);
    }
}
