package quorma.gameengine.effects;

import java.util.List;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/**
 * What the turn offers to an {@link Effect}: only what effects need, nothing of the rest of the turn code. The turn's
 * {@code Game} implements it. A method is added here when the effect that needs it moves out of {@code Game}.
 */
public interface EffectContext {

    /** The per-mille factor on healing {@code target} receives (1000 unless E51 reaches it). */
    int healPermille(Creature target);

    /** True if a bonus must not apply to {@code target} (E61). */
    boolean noBonus(Creature target);

    /** How many living allies of {@code side} stand in the lanes next to {@code creature}. */
    int adjacentAllyCount(Side side, Creature creature);

    /**
     * Damage by an effect (not combat): T21 replacements first, then T20 if it would destroy; the destruction or the
     * save is appended to {@code after}.
     *
     * @param source the creature whose ability deals it, null for a spell
     */
    List<StatChange> damage(Side sourceSide, Creature source, Side side, Creature target, int amount, Followups after);

    /** Destroys {@code target} (not damage), unless a T20 replacement saves it. */
    void destroy(Side sourceSide, Creature source, Side side, Creature target, Followups after);

    /** Marks the destruction of {@code target} in {@code after}: its log line and the destruction triggers. */
    void destroyed(Side side, Creature target, Side sourceSide, Creature source, Followups after);

    /** Puts {@code target} in its owner's discard pile, with no destruction. */
    void discard(Side side, Creature target, Followups after);

    /** If a T20 replacement saves {@code target} from destruction, applies it, logs the save in {@code after}, and says so. */
    boolean saveFromDestruction(Side side, Creature target, Followups after);
}
