package quorma.gameengine.turn;

import quorma.gameengine.model.Creature;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.SpellCard;

/**
 * A hook for what happens when a spell or an equipment is triggered in Refill, called after the engine has logged the
 * event. The engine itself applies the effects written on the card ({@code TurnResolver}); a hook adds to that, for
 * tests that record the calls.
 */
public interface CardEffects {

    /** A spell was drawn by {@code owner}. It takes effect now. */
    void onSpell(SpellCard card, Side owner);

    /** An equipment waiting for a creature was applied to {@code receiver}, a creature of {@code owner}. */
    void onEquipment(EquipmentCard card, Side owner, Creature receiver);

    /** Adds nothing to what the engine applies. */
    CardEffects NONE = new CardEffects() {
        @Override
        public void onSpell(SpellCard card, Side owner) {
            // the effects on the card are applied by the engine
        }

        @Override
        public void onEquipment(EquipmentCard card, Side owner, Creature receiver) {
            // the effects on the card are applied by the engine
        }
    };
}
