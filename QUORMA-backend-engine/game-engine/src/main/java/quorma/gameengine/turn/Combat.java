package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.List;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.effects.TriggerContext;
import quorma.gameengine.log.MatchEvent.AttackSkipped;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.log.MatchEvent.CreatureSaved;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.SkipReason;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Lasting;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Target;

/**
 * Combat (ruleset 9): Cycle 1 (simultaneous blows, then the redirected attacks one by one), Cycle 2 (one attack at a
 * time), the replacements before an attack (T39, T22) and what a blow does (rescue, damage, spill, triggers).
 */
final class Combat {

    private Combat() {}

    /** Cycle 1; see {@link TurnResolver#cycle1}. */
    static void cycle1(Game g, List<Attack> attacks, List<MatchEvent> log) {
        record Blow(Attack attack, Creature attacker, Creature target, Strike strike) {}
        MatchState state = g.state;
        g.cycle = 1;
        g.phase("Inizio del combattimento", log);
        g.phase("Ciclo 1", log);

        List<Attack> landing = new ArrayList<>();
        for (Attack attack : attacks) {
            if (!attack.inCycle1() || attack.isRedirected()) continue;
            Target targetRef = attack.target().orElseThrow();
            Creature attacker = state.player(attack.attacker()).creatureAt(attack.attackerLane()).orElse(null);
            Creature target = state.player(targetRef.side()).creatureAt(targetRef.lane()).orElse(null);
            if (attacker == null || target == null) continue;
            if (attacker.destroyed()) {
                log.add(new AttackSkipped(attack, 1, SkipReason.ATTACKER_DESTROYED));
                continue;
            }
            if (target.destroyed()) {
                log.add(new AttackSkipped(attack, 1, SkipReason.TARGET_DESTROYED));
                continue;
            }
            if (beforeAttack(g, attack, attacker, target, 1, log)) {
                landing.add(attack);
            }
        }

        // Every blow is worked out from the state at the start of the cycle, then they all land.
        Board b = g.board();
        CycleStart start = CycleStart.of(b);
        List<Blow> blows = new ArrayList<>();
        for (Attack attack : landing) {
            Target targetRef = attack.target().orElseThrow();
            Creature attacker = state.player(attack.attacker()).creatureAt(attack.attackerLane()).orElse(null);
            Creature target = state.player(targetRef.side()).creatureAt(targetRef.lane()).orElse(null);
            if (attacker == null || target == null || attacker.destroyed() || target.destroyed()) continue;
            Strike strike = Strike.of(g, b, start, attack.attacker(), attacker, targetRef.side(), target, true);
            blows.add(new Blow(attack, attacker, target, strike));
        }

        Windows windows = new Windows();
        for (Blow blow : blows) {
            boolean mutualKill = blow.strike().killed() && blows.stream().anyMatch(other ->
                    other != blow && other.strike().killed()
                            && other.attacker() == blow.target() && other.target() == blow.attacker());
            Strike strike = mutualKill ? blow.strike().withoutSpill() : blow.strike();
            land(g, blow.attack(), blow.attacker(), blow.target(), strike, 1, mutualKill, log, windows);
        }
        windows.fire(g, log);

        // 9.4: a redirected attack of Cycle 1 resolves after all the simultaneous ones, one at a time, in attack order.
        for (Attack attack : attacks) {
            if (attack.inCycle1() && attack.isRedirected()) {
                sequentialAttack(g, start, attack, 1, log);
            }
        }
    }

    /** Cycle 2 and the end of Combat; see {@link TurnResolver#cycle2}. */
    static void cycle2(Game g, List<Attack> attacks, List<MatchEvent> log) {
        g.cycle = 2;
        g.phase("Ciclo 2", log);
        CycleStart start = CycleStart.of(g.board());
        for (Attack attack : attacks) {
            if (!attack.inCycle1()) {
                sequentialAttack(g, start, attack, 2, log);
            }
        }
        g.cycle = 0;
        g.phase("Fine del combattimento", log);
        g.expire(Lasting.END_OF_COMBAT);
    }

    /**
     * One attack resolved on its own with immediate effect: the attacks of Cycle 2 (9.3) and the redirected ones of
     * Cycle 1 (9.4). An attacker already destroyed does not attack, nor does one whose target is gone, so a redirect
     * to a creature destroyed meanwhile does nothing. After the blow its triggers resolve before the next attack.
     */
    private static void sequentialAttack(Game g, CycleStart start, Attack attack, int cycle, List<MatchEvent> log) {
        MatchState state = g.state;
        Creature attacker = state.player(attack.attacker()).creatureAt(attack.attackerLane()).orElse(null);
        if (attacker == null || attacker.destroyed()) {
            log.add(new AttackSkipped(attack, cycle, SkipReason.ATTACKER_DESTROYED));
            return;
        }
        if (attack.target().isEmpty()) {
            log.add(new AttackSkipped(attack, cycle, SkipReason.NO_TARGET));
            return;
        }
        Target targetRef = attack.target().orElseThrow();
        Creature target = state.player(targetRef.side()).creatureAt(targetRef.lane()).orElse(null);
        if (target == null || target.destroyed()) {
            log.add(new AttackSkipped(attack, cycle, SkipReason.TARGET_DESTROYED));
            return;
        }
        if (!beforeAttack(g, attack, attacker, target, cycle, log)) {
            return;
        }
        if (attacker.destroyed() || attacker.leftPlay()) {
            log.add(new AttackSkipped(attack, cycle, SkipReason.ATTACKER_DESTROYED));
            return;
        }
        if (target.destroyed() || target.leftPlay()) {
            log.add(new AttackSkipped(attack, cycle, SkipReason.TARGET_DESTROYED));
            return;
        }
        Board b = g.board();
        Strike strike = Strike.of(g, b, start, attack.attacker(), attacker, targetRef.side(), target, true);
        Windows windows = new Windows();
        land(g, attack, attacker, target, strike, cycle, false, log, windows);
        windows.fire(g, log);
    }

    /**
     * Before an attack is made: T39 of the target and T22 of the attacker (replacements: E25 stops the attack, E23
     * stops the target from damaging the attacker for the rest of the Combat), then T6 of the attacker and T7 of the
     * target, resolved. Returns false if the attack does not happen (logged).
     */
    private static boolean beforeAttack(Game g, Attack attack, Creature attacker, Creature target, int cycle,
            List<MatchEvent> log) {
        Side attackerSide = attack.attacker();
        Side targetSide = attack.target().orElseThrow().side();
        Board b = g.board();
        for (Board.Active a : b.replacements("T39", target)) {
            if (a.e().effectId().equals("E25") && attackerSide != targetSide) {
                g.destruction.markReplacement(a, target);
                log.add(new AttackSkipped(attack, cycle, SkipReason.PREVENTED));
                return false;
            }
        }
        Boolean wouldKill = null;
        TriggerContext ctx = TriggerContext.attacking(targetSide, target, attackerSide, attacker);
        for (BoundExpansion e : b.abilities(attacker, "T22")) {
            if (!attacker.canTrigger(e, g.turn())) {
                continue;
            }
            if (wouldKill == null && e.conditions().stream().anyMatch(c -> c.id().equals("C21"))) {
                wouldKill = Strike.of(g, b, CycleStart.of(b), attackerSide, attacker, targetSide, target, false).killed();
            }
            if (!b.holds(e, attackerSide, attacker, new Board.Ctx(target, null, ctx, wouldKill))) {
                continue;
            }
            switch (e.effectId()) {
                case "E25" -> {
                    attacker.markTriggered(e, g.turn());
                    log.add(new AttackSkipped(attack, cycle, SkipReason.PREVENTED));
                    return false;
                }
                case "E23" -> {
                    attacker.markTriggered(e, g.turn());
                    target.preventDamageTo(attacker, e.abilityId(), g.lasting(e.duration(), attacker));
                }
                default -> { }
            }
        }
        g.fireAndResolve(List.of(new GameEvent.Attacks(attackerSide, attacker, targetSide, target)), log);
        return true;
    }

    /** What a blow sets off, fired once the blows of a cycle (or one sequential attack) have landed. */
    private static final class Windows {
        final List<GameEvent> destroyed = new ArrayList<>();
        final List<GameEvent> damaged = new ArrayList<>();
        final List<GameEvent> survived = new ArrayList<>();

        /** Destructions (T10, T19, T36, T37), then damage (T13, T38, T11), then survivors (T12). */
        void fire(Game g, List<MatchEvent> log) {
            g.fireAndResolve(destroyed, log);
            g.fireAndResolve(damaged, log);
            g.fireAndResolve(survived, log);
        }
    }

    /** A blow lands: rescue (T20), damage, spill, the log, and the triggers it sets off. */
    private static void land(Game g, Attack attack, Creature attacker, Creature target, Strike strike, int cycle,
            boolean mutualKill, List<MatchEvent> log, Windows windows) {
        Side attackerSide = attack.attacker();
        Target targetRef = attack.target().orElseThrow();
        Board.Active rescue = strike.killed() ? g.destruction.rescue(targetRef.side(), target) : null;
        if (rescue != null) {
            boolean keepSpill = rescue.e().effectId().equals("E11") && rescue.e().boolParam("spill_on_prevent", false);
            if (rescue.e().effectId().equals("E11")) {
                strike = strike.saved(target.currentDef(), keepSpill);
            } else {
                strike = strike.saved(target.currentDef(), false);
            }
        } else {
            target.takeDamage(strike.damage());
            if (strike.lethal()) {
                target.markDestroyed();
            }
        }
        if (strike.spill() > 0) {
            g.state.player(attackerSide.other()).loseLife(strike.spill());
        }
        log.add(strike.event(attack, cycle, mutualKill));
        if (strike.damage() > 0) {
            windows.damaged.add(new GameEvent.Damaged(targetRef.side(), target, strike.damage(), attackerSide, attacker, true));
            windows.damaged.add(new GameEvent.Dealt(attackerSide, attacker, targetRef.side(), target, strike.damage(),
                    strike.killed()));
        }
        if (strike.spill() > 0) {
            windows.damaged.add(new GameEvent.Spilled(attackerSide, attacker));
        }
        if (rescue != null) {
            log.add(new CreatureSaved(targetRef.side(), targetRef.lane(), target.card().id(), rescue.e().abilityId()));
        } else if (strike.killed()) {
            log.add(new CreatureDestroyed(targetRef.side(), targetRef.lane(), target.card().id()));
            windows.destroyed.add(new GameEvent.Destroyed(targetRef.side(), target, attackerSide, attacker));
            attacker.endUntilKill();
        } else if (strike.damage() > 0 && !target.destroyed()) {
            windows.survived.add(new GameEvent.Survived(targetRef.side(), target));
        }
    }
}
