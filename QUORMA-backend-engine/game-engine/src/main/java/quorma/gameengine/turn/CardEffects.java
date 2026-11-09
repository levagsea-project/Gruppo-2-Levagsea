package quorma.gameengine.turn;

import quorma.gameengine.model.Creature;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.SpellCard;

/**
 * What a spell or an equipment does when it is triggered in Refill. The engine logs the event and calls this
 * hook; the hook changes the game.
 */
public interface CardEffects {

    /** A spell was drawn by {@code owner}. It takes effect now. */
    void onSpell(SpellCard card, Side owner);

    /** An equipment waiting for a creature was applied to {@code receiver}, a creature of {@code owner}. */
    void onEquipment(EquipmentCard card, Side owner, Creature receiver);

    /** Does nothing. The default until card data exists. */
    CardEffects NONE = new CardEffects() {
        @Override
        public void onSpell(SpellCard card, Side owner) {
            // TODO(spells-equipment)
        }

        @Override
        public void onEquipment(EquipmentCard card, Side owner, Creature receiver) {
            // TODO(spells-equipment)
        }
    };
}
