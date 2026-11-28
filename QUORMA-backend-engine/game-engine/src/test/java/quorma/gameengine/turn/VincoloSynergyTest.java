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
 * Vincolo 3, 4 and 5 (ruleset 7.10) with the real hand evaluator. Numbers from {@code balance.json}: Vincolo 3 gives
 * +600 ATK, Vincolo 4 +400, Vincolo 5 +1000. Side A is Adattati, side B is SEKTAC (no advantage), every creature has
 * 1000 ATK. Every expected value is worked out by hand.
 */
class VincoloSynergyTest {

    private final TurnResolver resolver = new TurnResolver(TurnResolvers.RULES);

    private static MatchState match(String boardA, String boardB) {
        return Boards.match(
                Boards.side(Subfaction.ADATTATI, 12000, boardA), Boards.side(Subfaction.SEKTAC, 12000, boardB));
    }

    /** Side A's blows of Cycle 1, in lane order. */
    private List<AttackResolved> blowsOfA(MatchState state) {
        Evaluation evaluation = resolver.evaluate(state);
        List<MatchEvent> log = new ArrayList<>();
        resolver.cycle1(state, evaluation, resolver.declare(state, evaluation), log);
        return log.stream()
                .filter(e -> e instanceof AttackResolved)
                .map(e -> (AttackResolved) e)
                .filter(e -> e.attack().attacker() == Side.A)
                .toList();
    }

    private static Modifier bonus(String tier, int giverLane, String giverCard, int value) {
        return new Modifier(new CreatureRef(Side.A, giverLane, giverCard), tier, "E1", Modifier.Kind.ATK_BONUS, value);
    }

    @Test
    void threeVincoloActivateOnlyTheThirdTier() {
        MatchState state = match("1:2V(1000/9000) 2:3V(1000/9000) 3:4V(1000/9000)", "1:9F(100/9000) 2:KS(100/9000) 3:5T(100/9000)");
        Evaluation evaluation = resolver.evaluate(state);
        assertEquals(List.of("VINCOLO_3"), evaluation.synergies().tiers(Side.A));
        assertEquals(List.of(), evaluation.synergies().tiers(Side.B));
    }

    @Test
    void vincolo3GivesAnAdjacentVincoloItsBonus() {
        // High card: the 4 in lane 3 attacks. Lane 2 is a Vincolo next to it: 1000 + 600.
        List<AttackResolved> blows = blowsOfA(
                match("1:2V(1000/9000) 2:3V(1000/9000) 3:4V(1000/9000)", "1:9F(100/9000) 2:KS(100/9000) 3:5T(100/9000)"));

        assertEquals(1, blows.size());
        assertEquals(1600, blows.get(0).atk());
        assertEquals(1600, blows.get(0).damage());
        assertEquals(List.of(bonus("VINCOLO_3", 3, "4V", 600)), blows.get(0).modifiers());
    }

    @Test
    void vincolo3NeedsAnAdjacentVincolo() {
        // Lanes 1, 3 and 5: three Vincolo, none next to another. Lanes 1 and 5 are not adjacent.
        List<AttackResolved> blows = blowsOfA(
                match("1:2V(1000/9000) 3:3V(1000/9000) 5:4V(1000/9000)", "1:9F(100/9000) 3:KS(100/9000) 5:5T(100/9000)"));

        assertEquals(1, blows.size());
        assertEquals(1000, blows.get(0).atk());
        assertEquals(List.of(), blows.get(0).modifiers());
    }

    @Test
    void aDestroyedNeighbourStillCountsForTheTierButNoLongerGivesAdjacency() {
        // The destroyed 3 in lane 2 counts for the count (3 Vincolo) but is not a creature that satisfies the condition.
        List<AttackResolved> blows = blowsOfA(
                match("1:2V(1000/9000) 2:3V(1000/9000)! 3:4V(1000/9000)", "1:9F(100/9000) 2:KS(100/9000) 3:5T(100/9000)"));

        assertEquals(1, blows.size());
        assertEquals(1000, blows.get(0).atk());
        assertEquals(List.of(), blows.get(0).modifiers());
    }

    @Test
    void vincolo4AddsTheNeighboursBonusToEveryAttacker() {
        // Four of a suit: the three strongest (lanes 1-3) attack, lane 4 does not. Tiers 3 and 4 are active.
        //   lane 1: 1000 + 600 (tier 3) + 400 (tier 4, from lane 2)                = 2000
        //   lane 2: 1000 + 600 + 400 (from lane 1) + 400 (from lane 3)            = 2400
        //   lane 3: 1000 + 600 + 400 (from lane 2) + 400 (from lane 4)            = 2400
        
        List<AttackResolved> blows = blowsOfA(match(
                "1:5V(1000/9000) 2:4V(1000/9000) 3:3V(1000/9000) 4:2V(1000/9000)",
                "1:9F(100/9000) 2:KS(100/9000) 3:5T(100/9000) 4:JF(100/9000)"));

        assertEquals(List.of(2000, 2400, 2400), blows.stream().map(AttackResolved::atk).toList());
        // Modifiers are listed by the lane of the creature that has the effect, then in the order of its abilities.
        assertEquals(List.of(
                bonus("VINCOLO_4", 1, "5V", 400), bonus("VINCOLO_3", 2, "4V", 600), bonus("VINCOLO_4", 3, "3V", 400)),
                blows.get(1).modifiers());
    }

    @Test
    void vincolo5AddsItsOwnBonusAndLinksEveryVincolo() {
        // A straight flush: all five attack. Every Vincolo counts as adjacent to every other one (E34), so each
        // attacker gets: tier 3 +600, tier 4 +400 from each of the four others (1600), tier 5 +1000 = 4200.
        List<AttackResolved> blows = blowsOfA(match(
                "1:2V(1000/9000) 2:3V(1000/9000) 3:4V(1000/9000) 4:5V(1000/9000) 5:6V(1000/9000)",
                "1:9F(100/9000) 2:KS(100/9000) 3:5T(100/9000) 4:JF(100/9000) 5:8S(100/9000)"));

        assertEquals(List.of(4200, 4200, 4200, 4200, 4200), blows.stream().map(AttackResolved::atk).toList());
        // lane 1: its own tiers 3 and 5, then a tier-4 bonus from each of lanes 2 to 5 (lane 5 is not next to lane 1
        // but is linked by tier 5)
        List<Modifier> lane1 = blows.get(0).modifiers();
        assertEquals(6, lane1.size());
        assertEquals(bonus("VINCOLO_3", 1, "2V", 600), lane1.get(0));
        assertEquals(bonus("VINCOLO_5", 1, "2V", 1000), lane1.get(1));
        assertEquals(bonus("VINCOLO_4", 5, "6V", 400), lane1.get(5));
    }
}
