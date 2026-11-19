package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;
import quorma.gameengine.log.FizzleReason;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.AttackRedirected;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.log.MatchEvent.AttackSkipped;
import quorma.gameengine.log.MatchEvent.EffectFizzled;
import quorma.gameengine.log.SkipReason;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.model.Target;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * E24 with T9 (ruleset 9.4): a creature an attack was declared against aims it at the living creature of its own
 * player next to it. The redirected attack keeps the cycle of the original attack; in Cycle 1 it resolves after the
 * simultaneous ones, and a target that is gone by then means the attack does not happen. A is Adattati and carries
 * the redirect on its lane 1 (or 2) creature; B is SEKTAC, its Kings (a Pair) are the attackers.
 */
class RedirectTest {

    private final TurnResolver resolver = new TurnResolver(TurnResolvers.RULES);

    private static InlineEffect redirect() {
        return new InlineEffect(List.of(new ExpansionDef("T9", Map.of(), "E24",
                Map.of("new_target_id", new Value.StrV("S7")), "S1", Map.of(), "D_Istantanea", List.of(), ResClass.T)));
    }

    private static MatchState match(PlayerState a, String boardB) {
        return Boards.match(a, Boards.side(Subfaction.SEKTAC, 12000, boardB));
    }

    private static List<AttackResolved> blowsOfB(List<MatchEvent> log) {
        return log.stream().filter(e -> e instanceof AttackResolved).map(e -> (AttackResolved) e)
                .filter(e -> e.attack().attacker() == Side.B).toList();
    }

    @Test
    void aRedirectedAttackOfCycle1ResolvesAfterTheSimultaneousOnes() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2V(1000/9000) 2:3F(1000/9000) 3:4S(1000/9000)");
        Boards.give(a, 1, redirect());
        MatchState state = match(a, "1:KS(1000/90000) 3:KF(1000/90000)");
        Evaluation evaluation = resolver.evaluate(state);

        List<MatchEvent> opening = new ArrayList<>();
        List<Attack> attacks = resolver.declareTriggers(state, resolver.declare(state, evaluation), opening);

        Attack redirected = attacks.stream().filter(x -> x.attacker() == Side.B && x.attackerLane() == 1).findFirst().orElseThrow();
        assertEquals(Optional.of(new Target(Side.A, 2)), redirected.target());
        assertEquals(Optional.of(new Target(Side.A, 1)), redirected.redirectedFrom());
        assertTrue(redirected.inCycle1(), "it keeps the cycle it was declared in");
        assertTrue(opening.stream().anyMatch(e -> e instanceof AttackRedirected r
                && r.to().equals(new Target(Side.A, 2)) && r.abilityId().equals("INLINE_1")));

        List<MatchEvent> cycle1 = new ArrayList<>();
        resolver.cycle1(state, evaluation, attacks, cycle1);
        List<AttackResolved> blows = blowsOfB(cycle1);
        // the simultaneous blow (lane 3 on lane 3) first, then the redirected one (lane 1 on lane 2), both Cycle 1
        assertEquals(List.of(3, 1), blows.stream().map(b -> b.attack().attackerLane()).toList());
        assertEquals(List.of(3, 2), blows.stream().map(b -> b.attack().target().orElseThrow().lane()).toList());
        assertEquals(List.of(1, 1), blows.stream().map(AttackResolved::cycle).toList());
        assertEquals(8000, Boards.creature(a, 2).currentDef(), "the redirected blow landed on lane 2");
        assertEquals(9000, Boards.creature(a, 1).currentDef(), "and not on lane 1");
    }

    @Test
    void aRedirectedAttackOfCycle2StaysInCycle2() {
        // Lane 1 of A is empty: B's King aims at the first creature found, lane 2, which redirects to lane 3.
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "2:3F(1000/9000) 3:4S(1000/9000)");
        Boards.give(a, 2, redirect());
        MatchState state = match(a, "1:KS(1000/90000)");
        Evaluation evaluation = resolver.evaluate(state);
        List<Attack> attacks = resolver.declareTriggers(state, resolver.declare(state, evaluation), new ArrayList<>());

        List<MatchEvent> log = new ArrayList<>();
        resolver.cycle1(state, evaluation, attacks, log);
        resolver.cycle2(state, evaluation, attacks, log);

        AttackResolved blow = blowsOfB(log).get(0);
        assertEquals(2, blow.cycle());
        assertEquals(3, blow.attack().target().orElseThrow().lane());
    }

    @Test
    void aRedirectToACreatureDestroyedMeanwhileDoesNotHappen() {
        // B's lane 2 attack (20000) destroys A's lane 2 in Cycle 1. The attack redirected there from lane 1 resolves
        // after it, finds its target destroyed and is skipped.
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2V(1000/9000) 2:3F(1000/9000)");
        Boards.give(a, 1, redirect());
        MatchState state = match(a, "1:KS(1000/90000) 2:KF(20000/90000)");
        Evaluation evaluation = resolver.evaluate(state);
        List<Attack> attacks = resolver.declareTriggers(state, resolver.declare(state, evaluation), new ArrayList<>());

        List<MatchEvent> log = new ArrayList<>();
        resolver.cycle1(state, evaluation, attacks, log);

        assertTrue(Boards.creature(a, 2).destroyed());
        assertTrue(log.stream().anyMatch(e -> e instanceof AttackSkipped s && s.cycle() == 1
                && s.reason() == SkipReason.TARGET_DESTROYED && s.attack().attacker() == Side.B
                && s.attack().attackerLane() == 1));
        assertEquals(9000, Boards.creature(a, 1).currentDef(), "the original target was not hit either");
    }

    @Test
    void withoutALivingNeighbourTheRedirectFizzlesAndTheAttackStays() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2V(1000/9000)");
        Boards.give(a, 1, redirect());
        MatchState state = match(a, "1:KS(1000/90000)");
        Evaluation evaluation = resolver.evaluate(state);
        List<Attack> declared = resolver.declare(state, evaluation);

        List<MatchEvent> opening = new ArrayList<>();
        List<Attack> attacks = resolver.declareTriggers(state, declared, opening);

        assertEquals(declared, attacks);
        assertTrue(opening.stream().anyMatch(e -> e instanceof EffectFizzled f && f.reason() == FizzleReason.NO_NEW_TARGET));
    }
}
