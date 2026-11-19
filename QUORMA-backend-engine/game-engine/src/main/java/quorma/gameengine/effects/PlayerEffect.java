package quorma.gameengine.effects;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.log.TargetRef;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/**
 * What one effect does to a player, the deck, the discard pile or a lane: life (E8, E9), draws and mills (E35, E57),
 * returning or reordering cards (E36, E49), a pending effect on a lane (E64). One class per effect ID, found through
 * {@link PlayerEffects}. These effects aim at no creature, so they do not fit {@link Effect}.
 */
public interface PlayerEffect {

    /** The vocabulary ID this class implements, such as {@code "E8"}. Never renumbered. */
    String id();

    /** The resolution classes the effect is implemented for. */
    Set<ResClass> classes();

    /** The params the effect takes, per {@code mode} ({@code ""} when it has no mode), as {@link Effect#modes()}. */
    Map<String, ParamRule> modes();

    /**
     * Applies the effect.
     *
     * @return what to log as the effect's own line (the caller picks the event for a triggered effect or a card's),
     *         empty when the effect logs by itself (a mill or a draw) or has nothing to say
     */
    Optional<Resolution> apply(PlayerEffectContext ctx, Use use);

    /**
     * How a resolution was started.
     *
     * @param expansion  the effect's expansion
     * @param ownerSide  the side that owns the effect (its creature's side, or the card's)
     * @param playerSide the player the effect aims at: the owner for S31, S32, S47 and a lane, the other for S30
     * @param source     the creature whose ability it is, null for a spell
     * @param log        where an effect that logs by itself appends its events
     */
    record Use(BoundExpansion expansion, Side ownerSide, Side playerSide, Creature source, List<MatchEvent> log) {}

    /** The effect's own log line: who or what it hit, and the stats that changed. */
    record Resolution(TargetRef target, List<StatChange> changes) {}
}
