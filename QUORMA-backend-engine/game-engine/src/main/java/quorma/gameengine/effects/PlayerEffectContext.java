package quorma.gameengine.effects;

import java.util.List;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;

/** What the turn offers to a {@link PlayerEffect}: the players' state, and the two ways cards reach a lane. */
public interface PlayerEffectContext {

    /** The state of {@code side}'s player: life, deck, discard pile, lanes. */
    PlayerState player(Side side);

    /**
     * Draws the top card of {@code side}'s deck for {@code lane} (ruleset 10.4 to 10.6): a creature is placed there, a
     * spell resolves, an equipment waits. Returns true if the lane was filled.
     */
    boolean draw(Side side, int lane, List<MatchEvent> log);

    /** Places {@code creature} in its empty lane and runs what placing triggers; {@code drawn} says where it came from. */
    void place(Side side, Creature creature, boolean drawn, List<MatchEvent> log);

    /** A new creature of {@code card} for {@code lane}, with its abilities bound. */
    Creature newCreature(CreatureCard card, int lane);
}
