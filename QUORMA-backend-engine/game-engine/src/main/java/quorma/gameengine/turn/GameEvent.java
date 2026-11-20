package quorma.gameengine.turn;

import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** Something that happened and may trigger abilities. {@link Game#fire} turns events into stack pushes. */
public sealed interface GameEvent {

    /** {@code victim} was destroyed (T10 on it, T36 on its allies, T37 on the other side, T19 on the killer). */
    record Destroyed(Side side, Creature victim, Side killerSide, Creature killer) implements GameEvent {}

    /** {@code victim} took {@code amount} (> 0) damage (T13). {@code damager} is null for damage with no source. */
    record Damaged(Side side, Creature victim, int amount, Side damagerSide, Creature damager, boolean combat)
            implements GameEvent {}

    /** {@code attacker} dealt combat damage (> 0) to {@code victim} (T38). */
    record Dealt(Side side, Creature attacker, Side victimSide, Creature victim, int amount, boolean killed)
            implements GameEvent {}

    /** {@code creature} took combat damage and stayed in play (T12). */
    record Survived(Side side, Creature creature) implements GameEvent {}

    /** {@code attacker} dealt spill (T11). */
    record Spilled(Side side, Creature attacker) implements GameEvent {}

    /** An Evolution of {@code creature} fired (T15). */
    record EvolutionFired(Side side, Creature creature) implements GameEvent {}

    /** A spell of {@code side} was resolved (T18). */
    record SpellCast(Side side, String cardId) implements GameEvent {}

    /** A phase starts (T2). */
    record PhaseStart(String phase) implements GameEvent {}

    /** End of turn, after the defeat checks (T26). */
    record EndOfTurn() implements GameEvent {}

    /** {@code attacker} makes its attack on {@code target} (T6 on the attacker, T7 on the target). */
    record Attacks(Side side, Creature attacker, Side targetSide, Creature target) implements GameEvent {}
}
