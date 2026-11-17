package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.log.MatchEvent.CreatureSaved;
import quorma.gameengine.log.MatchEvent.EffectResolved;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * Vestigio 3, 4 and 5 (ruleset 7.10, 10) with the real hand evaluator. Numbers from {@code balance.json}: Vestigio 3
 * gives +1000 ATK, Vestigio 4 heals 1400 DEF, Vestigio 5 is locked until fewer than 5 Vestigio are in play after
 * Refill. Side A is Adattati, side B is SEKTAC (no advantage). The Vestigio of the tests are written {@code T}
 * (vesTigio). Every expected value is worked out by hand.
 */
class VestigioSynergyTest {

    private final TurnResolver resolver = new TurnResolver(TurnResolvers.RULES);

    private static MatchState match(PlayerState a, String boardB) {
        return Boards.match(a, Boards.side(Subfaction.SEKTAC, 12000, boardB));
    }

    private static List<EffectResolved> resolved(List<MatchEvent> log) {
        return log.stream().filter(e -> e instanceof EffectResolved).map(e -> (EffectResolved) e).toList();
    }

    @Test
    void vestigio4HealsLivingVestigioAtTheStartOfRefillUpToTheirBaseDef() {
        // Four Vestigio (the destroyed one counts): tiers 3 and 4 are active.
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000,
                "1:2T(1000/9000) 2:3T(1000/9000) 3:4T(1000/9000)! 4:5T(1000/9000)");
        MatchState state = match(a, "1:9F(100/9000)");
        Boards.creature(a, 1).takeDamage(6000); // 3000 left
        Boards.creature(a, 2).takeDamage(500); // 8500 left
        resolver.evaluate(state);

        List<MatchEvent> log = new ArrayList<>();
        resolver.refill(state, log);

        assertEquals(4400, Boards.creature(a, 1).currentDef(), "3000 + 1400");
        assertEquals(9000, Boards.creature(a, 2).currentDef(), "8500 + 1400, capped at the base DEF");
        assertEquals(9000, Boards.creature(a, 4).currentDef());
        List<EffectResolved> heals = resolved(log);
        assertTrue(heals.stream().anyMatch(h -> h.changes().equals(List.of(new StatChange(StatChange.Stat.DEF, 3000, 4400)))));
        assertTrue(heals.stream().noneMatch(h -> h.target().lane() == 3), "the destroyed creature is not healed");
    }

    @Test
    void vestigio3GivesTheReplacementABonusUntilItKills() {
        // Three Vestigio, the one in lane 1 destroyed. The card drawn into lane 1 gets +1000 ATK.
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000,
                "1:2T(1000/9000)! 2:3T(1000/9000) 3:4T(1000/9000)", "KS(1000/9000)");
        MatchState state = match(a, "1:9F(100/1000)");
        resolver.evaluate(state);

        List<MatchEvent> log = new ArrayList<>();
        resolver.refill(state, log);

        assertEquals(2000, Boards.creature(a, 1).currentAtk());
        EffectResolved bonus = resolved(log).stream().filter(e -> e.abilityId().equals("VESTIGIO_3")).findFirst().orElseThrow();
        assertEquals(new CreatureRef(Side.A, 1, "KS"), bonus.target());
        assertEquals(List.of(new StatChange(StatChange.Stat.ATK, 1000, 2000)), bonus.changes());

        // Next turn the King attacks with 2000 and kills the 1000 DEF creature opposite: the bonus ends there.
        Evaluation evaluation = resolver.evaluate(state);
        List<MatchEvent> combat = new ArrayList<>();
        resolver.cycle1(state, evaluation, resolver.declare(state, evaluation), combat);
        AttackResolved blow = combat.stream().filter(e -> e instanceof AttackResolved).map(e -> (AttackResolved) e)
                .filter(e -> e.attack().attacker() == Side.A).findFirst().orElseThrow();
        assertEquals(2000, blow.atk());
        assertTrue(blow.killed());
        assertEquals(1000, Boards.creature(a, 1).currentAtk());
    }

    @Test
    void aReplacementOfAnotherSuitGetsNothing() {
        // The destroyed creature in lane 1 is a Spira: no bonus for its replacement (C25 asks for a Vestigio).
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000,
                "1:2S(1000/9000)! 2:3T(1000/9000) 3:4T(1000/9000) 4:5T(1000/9000)", "KF(1000/9000)");
        MatchState state = match(a, "1:9F(100/1000)");
        resolver.evaluate(state);

        resolver.refill(state, new ArrayList<>());

        assertEquals(1000, Boards.creature(a, 1).currentAtk());
    }

    @Test
    void vestigio5SavesTheFirstCreatureOnlyAndRefillDestroysIt() {
        // A straight flush of Vestigio: tiers 3 and 4 active, tier 5 replaces tier 3. B has a pair of Kings that kill
        // the Vestigio in lanes 1 and 2 (20000 against 9000 DEF) in Cycle 1.
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000,
                "1:2T(1000/9000) 2:3T(1000/9000) 3:4T(1000/9000) 4:5T(1000/9000) 5:6T(1000/9000)");
        MatchState state = match(a, "1:KS(20000/90000) 2:KF(20000/90000)");
        Evaluation evaluation = resolver.evaluate(state);
        assertEquals(List.of("VESTIGIO_4", "VESTIGIO_5"), evaluation.synergies().tiers(Side.A));

        List<MatchEvent> cycle1 = new ArrayList<>();
        resolver.cycle1(state, evaluation, resolver.declare(state, evaluation), cycle1);

        // Lane 1 is saved: full DEF, not destroyed, no spill although 20000 - 9000 would have spilled.
        List<AttackResolved> blowsOfB = cycle1.stream().filter(e -> e instanceof AttackResolved).map(e -> (AttackResolved) e)
                .filter(e -> e.attack().attacker() == Side.B).toList();
        assertFalse(blowsOfB.get(0).killed());
        assertEquals(0, blowsOfB.get(0).spill());
        assertEquals(9000, blowsOfB.get(0).defAfter());
        assertTrue(cycle1.contains(new CreatureSaved(Side.A, 1, "2T", "VESTIGIO_5")));
        assertFalse(Boards.creature(a, 1).destroyed());
        assertTrue(Boards.creature(a, 1).condemned());
        assertTrue(a.isLocked("VESTIGIO_5"));

        // Lane 2 is destroyed: the lockout is set. Spill (20000 - 9000) x 1100 / 1000 = 12100 (a Pair).
        assertTrue(blowsOfB.get(1).killed());
        assertEquals(12100, blowsOfB.get(1).spill());
        assertTrue(Boards.creature(a, 2).destroyed());
        assertTrue(cycle1.contains(new CreatureDestroyed(Side.A, 2, "3T")));

        // Refill: the saved creature is destroyed now, both lanes are discarded, and with three Vestigio left (fewer
        // than 5) the lockout ends.
        List<MatchEvent> refill = new ArrayList<>();
        resolver.refill(state, refill);
        assertTrue(refill.contains(new CreatureDestroyed(Side.A, 1, "2T")));
        assertTrue(a.creatureAt(1).isEmpty() && a.creatureAt(2).isEmpty());
        assertFalse(a.isLocked("VESTIGIO_5"));
    }

    @Test
    void withoutFiveVestigioACreatureIsDestroyedAsUsual() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000,
                "1:2T(1000/9000) 2:3T(1000/9000) 3:4T(1000/9000) 4:5T(1000/9000)");
        MatchState state = match(a, "1:KS(20000/90000)");
        Evaluation evaluation = resolver.evaluate(state);
        assertFalse(evaluation.synergies().isActive(Side.A, "VESTIGIO_5"));

        List<MatchEvent> log = new ArrayList<>();
        resolver.cycle1(state, evaluation, resolver.declare(state, evaluation), log);

        assertTrue(Boards.creature(a, 1).destroyed());
        assertTrue(log.stream().noneMatch(e -> e instanceof CreatureSaved));
        assertFalse(a.isLocked("VESTIGIO_5"));
    }

    @Test
    void aLockoutLastsWhileFiveVestigioAreInPlayAfterRefill() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000,
                "1:2T(1000/9000) 2:3T(1000/9000) 3:4T(1000/9000) 4:5T(1000/9000) 5:6T(1000/9000)");
        a.lock("K", Suit.VESTIGIO, 5);
        a.releaseLocks();
        assertTrue(a.isLocked("K"), "five Vestigio: not fewer than 5");

        a.remove(5);
        a.releaseLocks();
        assertFalse(a.isLocked("K"), "four Vestigio: fewer than 5");
    }
}
