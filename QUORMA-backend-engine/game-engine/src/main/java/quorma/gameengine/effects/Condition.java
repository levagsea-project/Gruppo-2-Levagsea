package quorma.gameengine.effects;

import java.util.Optional;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/**
 * One condition of the vocabulary (C…): a test an ability must pass, now, for its effect to apply. One class per
 * condition ID, found through {@link Conditions}. A condition reads the table only through {@link ConditionContext}.
 */
public interface Condition {

    /** The vocabulary ID this class implements, such as {@code "C4"}. Never renumbered. */
    String id();

    /**
     * Checks the params as the card writes them, when the card is bound. Returns the problem, empty if there is none.
     * The default accepts any params.
     */
    default Optional<String> validate(ConditionDef def) {
        return Optional.empty();
    }

    /**
     * True if the condition holds now.
     *
     * @param ctx    what the condition may read
     * @param def    the condition as the card writes it (its params)
     * @param side   the side the ability belongs to
     * @param source the creature whose ability it is, null for an ability of the player
     */
    boolean holds(ConditionContext ctx, ConditionDef def, Side side, Creature source);
}
