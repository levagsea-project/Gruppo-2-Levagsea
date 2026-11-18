package quorma.gameengine.effects;

import quorma.gameengine.model.Creature;

/**
 * What the turn offers to an {@link Effect}: only what effects need, nothing of the rest of the turn code. The turn's
 * {@code Game} implements it. A method is added here when the effect that needs it moves out of {@code Game}.
 */
public interface EffectContext {

    /** The per-mille factor on healing {@code target} receives (1000 unless E51 reaches it). */
    int healPermille(Creature target);
}
