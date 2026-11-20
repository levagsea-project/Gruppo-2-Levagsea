package quorma.gameengine.effects;

import java.util.Optional;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/**
 * What a {@link Condition} may read: the table at one moment and the situation the ability is evaluated in (a blow, a
 * trigger). Built by the turn code for each evaluation; only what conditions need is here.
 */
public interface ConditionContext {

    /** True if {@code creature} of {@code side} attacks this turn (selected by the hand). */
    boolean attacker(Side side, Creature creature);

    /** How many creatures of {@code suit} {@code side} has, as counted at Valutazione. */
    int suitCount(Side side, Suit suit);

    /** True during the first cycle of combat. */
    boolean firstCycle();

    /** The type of the hand {@code side} showed at Valutazione, empty before the first Valutazione. */
    Optional<HandType> handType(Side side);

    /** True if a living creature of {@code suit}, other than {@code source}, is adjacent to {@code source}. */
    boolean hasAdjacentOfSuit(Side side, Creature source, Suit suit);

    /** True if the blow being evaluated would destroy its target (C21). */
    boolean wouldKill();

    /** True if the blow's target had more DEF than {@code source} had ATK at the start of the cycle (C22). */
    boolean targetDefAboveAtkAtCycleStart(Creature source);

    /** True if the lockout {@code key} of {@code side} is active. */
    boolean locked(Side side, String key);

    /** True if the creature that left {@code lane} of {@code side} was of {@code suit}. */
    boolean vacatedSuit(Side side, int lane, Suit suit);

    /** How many living allies of {@code side} are adjacent to {@code creature}. */
    int adjacentAllyCount(Side side, Creature creature);

    /** What the trigger of the ability provides ({@link TriggerContext#NONE} for a static one). */
    TriggerContext trigger();

    /** True if {@code destroyed}, which stood in {@code lane}, was adjacent to {@code source} when it was destroyed. */
    boolean wasAdjacent(Side side, Creature source, Creature destroyed, int lane);
}
