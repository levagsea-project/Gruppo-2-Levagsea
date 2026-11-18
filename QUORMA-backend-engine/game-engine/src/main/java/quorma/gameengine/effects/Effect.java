package quorma.gameengine.effects;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Lasting;
import quorma.gameengine.model.Side;

/**
 * What one effect of the vocabulary does to a creature. One class per effect ID (modes are branches inside the
 * class), found by ID through {@link Effects}. The trigger side is not here: the stack decides when an effect
 * resolves, and the turn code (through {@link EffectContext}) turns an {@link Outcome} into log events.
 */
public interface Effect {

    /** The vocabulary ID this class implements, such as {@code "E6"}. Never renumbered. */
    String id();

    /** The resolution classes the effect is implemented for (the card's ability must be one of them). */
    Set<ResClass> classes();

    /**
     * The params the effect takes, per {@code mode} ({@code ""} when it has no mode). {@link EffectRegistry} checks
     * a card against these when it is bound.
     */
    Map<String, ParamRule> modes();

    /**
     * Checks beyond what {@link #modes()} says, for the effect's own rules. Returns the problem, empty if none. The
     * default has none.
     *
     * @param mode   the {@code mode} param, {@code ""} if the card has none
     * @param params the effect params as written on the card
     */
    default Optional<String> validate(String mode, Map<String, Value> params) {
        return Optional.empty();
    }

    /**
     * Applies the effect to {@code target}, a living creature.
     *
     * @param ctx    what the turn offers to an effect
     * @param use    how this resolution was started
     * @param target the creature it affects
     */
    Outcome apply(EffectContext ctx, Use use, Creature target);

    /**
     * How an effect resolution was started: by an ability on the stack, or at once (a spell, an equipment or an
     * effect waiting on a lane).
     *
     * @param expansion  the effect's expansion, already scaled by E19 when it is triggered
     * @param ownerSide  the side that owns the effect (the side of the source creature, or of the card)
     * @param targetSide the side of the creature it affects
     * @param source     the creature whose ability it is, null for an immediate effect
     * @param lasting    how long what it does lasts
     * @param context    what the trigger provides, {@link TriggerContext#NONE} for an immediate effect
     * @param after      where destruction and other follow-ups of the effect are collected
     */
    record Use(BoundExpansion expansion, Side ownerSide, Side targetSide, Creature source, Lasting lasting,
            TriggerContext context, Followups after) {}
}
