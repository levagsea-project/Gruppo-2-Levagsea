package quorma.gameengine.turn;

import java.util.List;
import java.util.function.Supplier;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.effects.Followups;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.log.MatchEvent.CreatureDiscarded;
import quorma.gameengine.log.MatchEvent.CreatureSaved;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;

/**
 * Damage and destruction by effects (not combat): T21 replacements (E29), T20 rescues (E11, E12), what a destroyed
 * creature leaves behind for its source and the triggers, and discarding with no destruction. Appends log lines and
 * {@link GameEvent}s to the effect's {@link Followups}.
 */
final class Destruction {

    private final MatchState state;
    private final Supplier<Board> board;

    Destruction(MatchState state, Supplier<Board> board) {
        this.state = state;
        this.board = board;
    }

    boolean saveFromDestruction(Side side, Creature t, Followups after) {
        Board.Active rescue = rescue(side, t);
        if (rescue == null) {
            return false;
        }
        after.logs().add(new CreatureSaved(side, t.lane(), t.card().id(), rescue.e().abilityId()));
        return true;
    }

    /**
     * Damage by an effect (not combat): T21 replacements first (E29), then T20 if it would destroy. Logs the
     * destruction or the save after the effect's own line, and fires T13 and the destruction triggers.
     *
     * @param source the creature whose ability deals it, null for a spell
     */
    List<StatChange> damage(Side sourceSide, Creature source, Side side, Creature t, int amount, Followups after) {
        int before = t.currentDef();
        if (t.destroyed() || amount <= 0) {
            return List.of(new StatChange(StatChange.Stat.DEF, before, before));
        }
        amount = preventDamage(t, amount).left();
        if (amount <= 0) {
            return List.of(new StatChange(StatChange.Stat.DEF, before, before));
        }
        after.events().add(new GameEvent.Damaged(side, t, amount, sourceSide, source, false));
        if (amount >= before && saveFromDestruction(side, t, after)) {
            return List.of(new StatChange(StatChange.Stat.DEF, before, t.currentDef()));
        }
        t.takeDamage(amount);
        if (t.destroyed()) {
            destroyed(side, t, sourceSide, source, after);
        }
        return List.of(new StatChange(StatChange.Stat.DEF, before, t.currentDef()));
    }

    /** What is left of some damage after T21 replacements, and the last ability that prevented part of it. */
    record Prevention(int left, String abilityId) {}

    /** T21 with E29: prevents the next damage ({@code once}) or part of it ({@code amount}). */
    Prevention preventDamage(Creature t, int amount) {
        String by = null;
        for (Board.Active a : board.get().replacements("T21", t)) {
            if (!a.e().effectId().equals("E29") || amount <= 0) {
                continue;
            }
            markReplacement(a, t);
            by = a.e().abilityId();
            amount = a.e().boolParam("once", false) ? 0 : Math.max(0, amount - a.e().intParam("amount", 0));
        }
        return new Prevention(amount, by);
    }

    /** E10: destroys {@code t} (not damage), unless a T20 replacement saves it. */
    void destroy(Side sourceSide, Creature source, Side side, Creature t, Followups after) {
        if (t.destroyed()) {
            return;
        }
        if (saveFromDestruction(side, t, after)) {
            return;
        }
        t.markDestroyed();
        destroyed(side, t, sourceSide, source, after);
    }

    void destroyed(Side side, Creature t, Side sourceSide, Creature source, Followups after) {
        after.logs().add(new CreatureDestroyed(side, t.lane(), t.card().id()));
        after.events().add(new GameEvent.Destroyed(side, t, source != null ? sourceSide : null, source));
        if (source != null) {
            source.endUntilKill();
        }
    }

    /** E13: puts {@code t} in its owner's discard pile, with no destruction (no T10, no T20). */
    void discard(Side side, Creature t, Followups after) {
        PlayerState p = state.player(side);
        p.remove(t.lane());
        p.discard().add(t.card());
        after.logs().add(new CreatureDiscarded(side, t.lane(), t.card().id()));
    }

    /**
     * T20 "about to be destroyed": the first replacement that saves {@code target} (E12: DEF back to the base DEF,
     * destroyed at the next Refill if {@code then} says so, a lockout set; E11: DEF set to {@code set_def}). A creature
     * already saved and waiting for Refill is not saved again. Returns the replacement, or null.
     */
    Board.Active rescue(Side side, Creature target) {
        if (target.condemned()) {
            return null;
        }
        for (Board.Active a : board.get().replacements("T20", target)) {
            BoundExpansion e = a.e();
            switch (e.effectId()) {
                case "E12" -> {
                    if (e.stringParam("lockout.key").isPresent()) {
                        state.player(side).lock(e.stringParam("lockout.key").orElseThrow(),
                                Suit.valueOf(e.stringParam("lockout.suit").orElseThrow().toUpperCase(java.util.Locale.ROOT)),
                                e.intParam("lockout.reactivate_below"));
                    }
                    target.saveWithFullDef("destroy_at_next_refill".equals(e.stringParam("then").orElse("none")));
                }
                case "E11" -> {
                    target.setDef(e.intParam("set_def"));
                    target.undestroy();
                }
                default -> {
                    continue;
                }
            }
            markReplacement(a, target);
            return a;
        }
        return null;
    }

    /** Counts a replacement that applied: on its aura, or on the creature whose ability it is. */
    void markReplacement(Board.Active a, Creature c) {
        if (a.aura() != null) {
            a.aura().markTriggered();
        } else {
            (a.source() != null ? a.source() : c).markTriggered(a.e(), state.turn());
        }
    }
}
