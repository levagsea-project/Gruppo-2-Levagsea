package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.log.Modifier;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * Frattura 3, 4 and 5 (ruleset 7.10) with the real hand evaluator. Numbers from {@code balance.json}: Frattura 3 gives
 * +1200 ATK; Frattura 4 multiplies the spill multiplier by 1200 and treats the target's DEF as 800 per mille;
 * Frattura 5 does the same with 500 per mille. Side A is Adattati, side B is SEKTAC (no advantage), every creature of A
 * has 1000 ATK. Every expected value is worked out by hand.
 */
class FratturaSynergyTest {

    private final TurnResolver resolver = new TurnResolver(TurnResolvers.RULES);

    private static MatchState match(String boardA, String boardB) {
        return Boards.match(
                Boards.side(Subfaction.ADATTATI, 12000, boardA), Boards.side(Subfaction.SEKTAC, 12000, boardB));
    }

    /** Side A's blows of both cycles, in the order they happen. */
    private List<AttackResolved> blowsOfA(MatchState state) {
        Evaluation evaluation = resolver.evaluate(state);
        List<MatchEvent> log = new ArrayList<>();
        var attacks = resolver.declare(state, evaluation);
        resolver.cycle1(state, evaluation, attacks, log);
        resolver.cycle2(state, evaluation, attacks, log);
        return log.stream()
                .filter(e -> e instanceof AttackResolved)
                .map(e -> (AttackResolved) e)
                .filter(e -> e.attack().attacker() == Side.A)
                .toList();
    }

    private static Modifier modifier(String tier, int lane, String card, String effect, Modifier.Kind kind, int value) {
        return new Modifier(new CreatureRef(Side.A, lane, card), tier, effect, kind, value);
    }

    @Test
    void frattura3GivesTheBonusWhenTheTargetsDefIsAboveTheAttackersAtk() {
        // Three Frattura, high card: the 4 in lane 3 attacks the 5 in lane 3 with DEF 5000 > ATK 1000: 1000 + 1200.
        List<AttackResolved> blows = blowsOfA(
                match("1:2F(1000/9000) 2:3F(1000/9000) 3:4F(1000/9000)", "1:9V(100/9000) 2:KS(100/9000) 3:5T(100/5000)"));

        assertEquals(1, blows.size());
        assertEquals(2200, blows.get(0).atk());
        assertEquals(List.of(modifier("FRATTURA_3", 3, "4F", "E1", Modifier.Kind.ATK_BONUS, 1200)), blows.get(0).modifiers());
    }

    @Test
    void frattura3NeedsTheDefStrictlyAboveTheAtk() {
        // DEF 1000 is not above ATK 1000: no bonus. The blow kills (DEF 1000 - 1000), so 0 spill.
        List<AttackResolved> blows = blowsOfA(
                match("1:2F(1000/9000) 2:3F(1000/9000) 3:4F(1000/9000)", "1:9V(100/9000) 2:KS(100/9000) 3:5T(100/1000)"));

        assertEquals(1000, blows.get(0).atk());
        assertEquals(List.of(), blows.get(0).modifiers());
    }

    @Test
    void frattura3ComparesTheValuesTakenAtTheStartOfTheCycle() {
        // Two Frattura 5s attack the same lone creature (lane 5, DEF 3000) in Cycle 2. Both compare the DEF it had when
        // the cycle started (3000 > 1000), so both get +1200 even though the first blow leaves it at 800.
        List<AttackResolved> blows = blowsOfA(match("1:5F(1000/9000) 2:5F(1000/9000) 3:2F(1000/9000)", "5:KT(100/3000)"));

        assertEquals(2, blows.size());
        assertEquals(List.of(2, 2), blows.stream().map(AttackResolved::cycle).toList());
        assertEquals(List.of(2200, 2200), blows.stream().map(AttackResolved::atk).toList());
        assertEquals(800, blows.get(0).defAfter());
        assertEquals(800, blows.get(1).defBefore());
    }

    @Test
    void frattura4RaisesTheSpillMultiplierAndShrinksTheDefSpillUses() {
        // Four of a suit (multiplier 1350), all four attack. Lane 1: ATK 1000 kills a target with DEF 500.
        // Multiplier 1350 x 1200 / 1000 = 1620. Spill uses DEF 500 x 800 / 1000 = 400: (1000 - 400) x 1620 / 1000 = 972.
        List<AttackResolved> blows = blowsOfA(match(
                "1:2F(1000/9000) 2:3F(1000/9000) 3:4F(1000/9000) 4:5F(1000/9000)",
                "1:9V(100/500) 2:KS(100/500) 3:5T(100/500) 4:JV(100/500)"));

        AttackResolved first = blows.get(0);
        assertEquals(1000, first.damage());
        assertEquals(1620, first.spillMultiplierPermille());
        assertEquals(972, first.spill());
        assertEquals(List.of(
                modifier("FRATTURA_4", 1, "2F", "E21", Modifier.Kind.SPILL_MULTIPLIER, 1200),
                modifier("FRATTURA_4", 1, "2F", "E22", Modifier.Kind.SPILL_DEF_PERMILLE, 800)), first.modifiers());
    }

    @Test
    void frattura5ReplacesTier4() {
        // A straight flush (multiplier 2600), all five attack. Tier 5 replaces tier 4: 2600 x 1200 / 1000 = 3120, and
        // spill uses DEF 500 x 500 / 1000 = 250: (1000 - 250) x 3120 / 1000 = 2340. Only tier 5's modifiers are listed.
        List<AttackResolved> blows = blowsOfA(match(
                "1:2F(1000/9000) 2:3F(1000/9000) 3:4F(1000/9000) 4:5F(1000/9000) 5:6F(1000/9000)",
                "1:9V(100/500) 2:KS(100/500) 3:5T(100/500) 4:JV(100/500) 5:8S(100/500)"));

        AttackResolved first = blows.get(0);
        assertEquals(3120, first.spillMultiplierPermille());
        assertEquals(2340, first.spill());
        assertEquals(List.of(
                modifier("FRATTURA_5", 1, "2F", "E22", Modifier.Kind.SPILL_DEF_PERMILLE, 500),
                modifier("FRATTURA_5", 1, "2F", "E21", Modifier.Kind.SPILL_MULTIPLIER, 1200)), first.modifiers());
    }

    @Test
    void theSpillChangesOnlyApplyToAKill() {
        // Lane 1 hits DEF 9000 (above its ATK, so tier 3 gives +1200) for 2200: no kill, no spill, and only the ATK
        // bonus is listed, no spill modifier.
        List<AttackResolved> blows = blowsOfA(match(
                "1:2F(1000/9000) 2:3F(1000/9000) 3:4F(1000/9000) 4:5F(1000/9000)",
                "1:9V(100/9000) 2:KS(100/9000) 3:5T(100/9000) 4:JV(100/9000)"));

        assertEquals(0, blows.get(0).spill());
        assertEquals(List.of(modifier("FRATTURA_3", 1, "2F", "E1", Modifier.Kind.ATK_BONUS, 1200)), blows.get(0).modifiers());
    }
}
