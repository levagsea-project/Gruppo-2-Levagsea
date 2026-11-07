package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static quorma.gameengine.testkit.Attacks.hit;
import static quorma.gameengine.testkit.Attacks.noTarget;
import static quorma.gameengine.testkit.Evaluators.evaluation;
import static quorma.gameengine.testkit.Evaluators.hand;

import java.util.List;
import org.junit.jupiter.api.Test;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * Declare (ruleset 8): each attacker chosen by the hand takes a target, frozen from then on. The attack list is
 * ordered by attacker lane with both players interleaved; in one lane Potenze Artificiali come before
 * Resistenza Umana, and with the same faction player A comes first.
 *
 * <p>Side A is Resistenza Umana (Adattati) and side B is Potenze Artificiali (SEKTAC) unless a test says otherwise.
 */
class DeclareTest {

    private static final String FULL_A = "1:4V(1000/1000) 2:4F(1000/1000) 3:7S(1000/1000) 4:9T(1000/1000) 5:JV(1000/1000)";
    private static final String FULL_B = "1:2V(1000/1000) 2:3F(1000/1000) 3:KS(1000/1000) 4:5T(1000/1000) 5:6V(1000/1000)";

    private final TurnResolver resolver = TurnResolvers.noEvaluation();

    private List<Attack> declare(PlayerState a, PlayerState b, HandResult handA, HandResult handB) {
        return resolver.declare(Boards.match(a, b), evaluation(handA, 1000, handB, 1000));
    }

    @Test
    void everyAttackerOfEachHandDeclaresOneAttackInLaneOrderAcrossPlayers() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, FULL_A);
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, FULL_B);

        List<Attack> attacks = declare(a, b, hand(HandType.TWO_PAIR, 2, 4), hand(HandType.HIGH_CARD, 3));

        // lane 2 (A), lane 3 (B), lane 4 (A): ordered by attacker lane, players interleaved
        assertEquals(List.of(hit(Side.A, 2, 2), hit(Side.B, 3, 3), hit(Side.A, 4, 4)), attacks);
        assertTrue(attacks.stream().allMatch(Attack::inCycle1));
    }

    @Test
    void inOneLanePotenzeArtificialiComeBeforeResistenzaUmana() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, FULL_A); // Resistenza Umana
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, FULL_B); // Potenze Artificiali

        List<Attack> attacks = declare(a, b, hand(HandType.HIGH_CARD, 3), hand(HandType.HIGH_CARD, 3));

        assertEquals(List.of(hit(Side.B, 3, 3), hit(Side.A, 3, 3)), attacks);
    }

    @Test
    void withTheSameFactionPlayerAComesFirst() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, FULL_A);
        PlayerState b = Boards.side(Subfaction.PORTANOME, 12000, FULL_B); // also Resistenza Umana

        List<Attack> attacks = declare(a, b, hand(HandType.HIGH_CARD, 3), hand(HandType.HIGH_CARD, 3));

        assertEquals(List.of(hit(Side.A, 3, 3), hit(Side.B, 3, 3)), attacks);
    }

    @Test
    void anEmptyOppositeLaneScansUpwardAndWraps() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "4:9T(1000/1000)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, "1:2V(1000/1000) 2:3F(1000/1000)");

        // lane 4 checks 4, 5, 1: B's lane 1 is the first creature
        List<Attack> attacks = declare(a, b, hand(HandType.HIGH_CARD, 4), HandResult.none());

        assertEquals(List.of(hit(Side.A, 4, 1)), attacks);
        assertEquals(2, attacks.get(0).cycle());
    }

    @Test
    void theScanLooksOnlyAtTheEnemyLanes() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "2:4F(1000/1000) 3:7S(1000/1000)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, "3:KS(1000/1000)");

        // A's lane 2: B's lane 2 is empty, B's lane 3 has a creature (A's own lane 3 does not matter)
        List<Attack> attacks = declare(a, b, hand(HandType.PAIR, 2, 3), HandResult.none());

        assertEquals(List.of(hit(Side.A, 2, 3), hit(Side.A, 3, 3)), attacks);
    }

    @Test
    void anAttackerWithNoEnemyCreatureHasNoTarget() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "2:4F(1000/1000)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, "");

        List<Attack> attacks = declare(a, b, hand(HandType.HIGH_CARD, 2), HandResult.none());

        assertEquals(List.of(noTarget(Side.A, 2)), attacks);
    }

    @Test
    void destroyedCreaturesAreNotTargets() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "3:7S(1000/1000)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, "3:KS(1000/1000)! 5:6V(1000/1000)");

        List<Attack> attacks = declare(a, b, hand(HandType.HIGH_CARD, 3), HandResult.none());

        assertEquals(List.of(hit(Side.A, 3, 5)), attacks);
    }

    @Test
    void anEnemyWithOnlyDestroyedCreaturesGivesNoTarget() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "3:7S(1000/1000)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, "3:KS(1000/1000)!");

        assertEquals(List.of(noTarget(Side.A, 3)), declare(a, b, hand(HandType.HIGH_CARD, 3), HandResult.none()));
    }

    @Test
    void aHandWithNoAttackersDeclaresNothing() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, FULL_A);
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, FULL_B);

        assertEquals(List.of(), declare(a, b, HandResult.none(), HandResult.none()));
    }

    @Test
    void declaringChangesNothingOnTheTable() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, FULL_A);
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, FULL_B);
        MatchState state = Boards.match(a, b);

        resolver.declare(state, evaluation(hand(HandType.PAIR, 1, 2), 1100, hand(HandType.PAIR, 3, 4), 1100));

        assertEquals(12000, a.life());
        assertEquals(12000, b.life());
        for (int lane = 1; lane <= 5; lane++) {
            assertEquals(1000, Boards.creature(a, lane).currentDef());
            assertEquals(1000, Boards.creature(b, lane).currentDef());
        }
        assertEquals(1, state.turn());
    }
}
