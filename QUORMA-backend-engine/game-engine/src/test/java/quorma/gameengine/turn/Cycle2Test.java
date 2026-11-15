package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static quorma.gameengine.testkit.Attacks.hit;
import static quorma.gameengine.testkit.Blows.plain;
import static quorma.gameengine.testkit.Attacks.noTarget;
import static quorma.gameengine.testkit.Evaluators.evaluation;
import static quorma.gameengine.testkit.Evaluators.hand;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.AttackSkipped;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.log.SkipReason;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * Cycle 2 (ruleset 9.3): the attackers that did not attack in Cycle 1 attack one at a time,
 * in the order of the list (attacker lane order, players interleaved, as {@code declare} returns it). Each attack
 * applies at once. If the target is destroyed, or the attacker was destroyed before its turn, there is no attack.
 * Spill is always normal. All expected values are worked out by hand.
 *
 * <p>Same sides and multipliers as {@link Cycle1Test}: A is Adattati (Pair, 1100), B is SEKTAC (High card, 1000).
 */
class Cycle2Test {

    private final TurnResolver resolver = TurnResolvers.noEvaluation();
    private final Evaluation pairAgainstHighCard = evaluation(hand(HandType.PAIR, 1), 1100, hand(HandType.HIGH_CARD, 1), 1000);

    private List<MatchEvent> cycle2(MatchState state, List<Attack> attacks) {
        List<MatchEvent> log = new ArrayList<>();
        resolver.cycle2(state, pairAgainstHighCard, attacks, log);
        return log;
    }

    private static MatchState match(String boardA, String boardB) {
        return Boards.match(
                Boards.side(Subfaction.ADATTATI, 12000, boardA), Boards.side(Subfaction.SEKTAC, 12000, boardB));
    }

    @Test
    void anAttackerWhoseOppositeLaneIsEmptyHitsTheTargetItFound() {
        MatchState state = match("2:4V(4000/3000)", "4:9F(1000/1500)");

        List<MatchEvent> log = cycle2(state, List.of(hit(Side.A, 2, 4)));

        // spill (4000 - 1500) * 1100 / 1000 = 2750
        assertEquals(
                List.of(
                        plain(hit(Side.A, 2, 4), 2, 4000, 1000, 4000, 1500, -2500, true, 2750, false, 1100),
                        new CreatureDestroyed(Side.B, 4, "9F")),
                log);
        assertEquals(12000 - 2750, state.player(Side.B).life());
    }

    @Test
    void attacksAreAppliedOneAtATimeSoALaterOneSeesTheDefLeftByAnEarlierOne() {
        // both A1 and A2 end up on B3 (DEF 5000). A1 hits for 3000, leaving 2000; A2 hits for 4000 and kills
        MatchState state = match("1:4V(3000/3000) 2:4F(4000/3000)", "3:9F(1000/5000)");

        List<MatchEvent> log = cycle2(state, List.of(hit(Side.A, 1, 3), hit(Side.A, 2, 3)));

        // A2's spill uses the DEF just before its own blow: (4000 - 2000) * 1100 / 1000 = 2200 (not 4000 - 5000)
        assertEquals(
                List.of(
                        plain(hit(Side.A, 1, 3), 2, 3000, 1000, 3000, 5000, 2000, false, 0, false, 1100),
                        plain(hit(Side.A, 2, 3), 2, 4000, 1000, 4000, 2000, -2000, true, 2200, false, 1100),
                        new CreatureDestroyed(Side.B, 3, "9F")),
                log);
        assertEquals(12000 - 2200, state.player(Side.B).life());
    }

    @Test
    void anAttackerDoesNotAttackAnAlreadyDestroyedTarget() {
        // A1 kills B3 (spill (6000 - 5000) * 1100 / 1000 = 1100); A2 aimed at the same creature and does nothing
        MatchState state = match("1:4V(6000/3000) 2:4F(4000/3000)", "3:9F(1000/5000)");

        List<MatchEvent> log = cycle2(state, List.of(hit(Side.A, 1, 3), hit(Side.A, 2, 3)));

        assertEquals(
                List.of(
                        plain(hit(Side.A, 1, 3), 2, 6000, 1000, 6000, 5000, -1000, true, 1100, false, 1100),
                        new CreatureDestroyed(Side.B, 3, "9F"),
                        new AttackSkipped(hit(Side.A, 2, 3), 2, SkipReason.TARGET_DESTROYED)),
                log);
        assertEquals(12000 - 1100, state.player(Side.B).life(), "no second spill and no new target");
    }

    @Test
    void aTargetAlreadyDestroyedInCycle1IsNotAttacked() {
        MatchState state = match("1:4V(4000/3000)", "3:9F(1000/1500)!");

        List<MatchEvent> log = cycle2(state, List.of(hit(Side.A, 1, 3)));

        assertEquals(List.of(new AttackSkipped(hit(Side.A, 1, 3), 2, SkipReason.TARGET_DESTROYED)), log);
        assertEquals(12000, state.player(Side.B).life());
    }

    @Test
    void anAttackerDestroyedBeforeItsTurnDoesNotAttack() {
        // lane 1: A1 attacks B3 and kills it. Lane 3: B3 would attack A1, but it is destroyed when called
        MatchState state = match("1:4V(4000/1500)", "3:9F(3000/2000)");

        List<MatchEvent> log = cycle2(state, List.of(hit(Side.A, 1, 3), hit(Side.B, 3, 1)));

        // A1's spill (4000 - 2000) * 1100 / 1000 = 2200, and it is a normal spill even though B3 would have hit back
        assertEquals(
                List.of(
                        plain(hit(Side.A, 1, 3), 2, 4000, 1000, 4000, 2000, -2000, true, 2200, false, 1100),
                        new CreatureDestroyed(Side.B, 3, "9F"),
                        new AttackSkipped(hit(Side.B, 3, 1), 2, SkipReason.ATTACKER_DESTROYED)),
                log);
        assertEquals(12000 - 2200, state.player(Side.B).life());
        assertEquals(12000, state.player(Side.A).life());
        assertEquals(1500, Boards.creature(state.player(Side.A), 1).currentDef(), "A1 was never hit");
    }

    @Test
    void aDestroyedAttackerWithoutATargetIsReportedAsDestroyedNotAsNoTarget() {
        // B3 is destroyed by A1 first; when called it has no target either, and the destroyed check comes first
        MatchState state = match("1:4V(4000/1500)", "3:9F(3000/2000)");

        List<MatchEvent> log = cycle2(state, List.of(hit(Side.A, 1, 3), noTarget(Side.B, 3)));

        assertEquals(new AttackSkipped(noTarget(Side.B, 3), 2, SkipReason.ATTACKER_DESTROYED), log.get(2));
        assertEquals(3, log.size());
    }

    @Test
    void anAttackerDestroyedBeforeItsTurnWhoseTargetIsAlsoGoneIsReportedAsDestroyedAttacker() {
        // B3 is destroyed by A1, and B3's target A2 is destroyed by B4: both are gone, the attacker is reported
        MatchState state = match("1:4V(4000/1500) 2:5V(1000/500)", "3:9F(3000/2000) 4:8F(2000/2000)");

        List<MatchEvent> log = cycle2(state, List.of(hit(Side.A, 1, 3), hit(Side.B, 4, 2), hit(Side.B, 3, 2)));

        assertEquals(new AttackSkipped(hit(Side.B, 3, 2), 2, SkipReason.ATTACKER_DESTROYED), log.get(log.size() - 1));
    }

    @Test
    void anAttackWithoutATargetIsSkipped() {
        MatchState state = match("2:4V(4000/3000)", "");

        List<MatchEvent> log = cycle2(state, List.of(noTarget(Side.A, 2)));

        assertEquals(List.of(new AttackSkipped(noTarget(Side.A, 2), 2, SkipReason.NO_TARGET)), log);
        assertEquals(12000, state.player(Side.B).life());
    }

    @Test
    void aCycle1AttackDoesNotAttackAgain() {
        MatchState state = match("3:4V(4000/3000)", "3:9F(1000/1500)");

        assertEquals(List.of(), cycle2(state, List.of(hit(Side.A, 3, 3))));
        assertEquals(1500, Boards.creature(state.player(Side.B), 3).currentDef());
    }

    @Test
    void attacksAreResolvedInTheGivenOrderAlternatingPlayers() {
        MatchState state = match("1:4V(1000/9000) 4:JV(1000/9000)", "2:9F(1000/9000) 3:5T(1000/9000) 5:6S(1000/9000)");
        // as declare returns them: attacker lane 1 (A), lane 2 (B), lane 4 (A)
        List<Attack> attacks = List.of(hit(Side.A, 1, 3), hit(Side.B, 2, 4), hit(Side.A, 4, 5));

        List<MatchEvent> log = cycle2(state, attacks);

        assertEquals(
                List.of(
                        plain(hit(Side.A, 1, 3), 2, 1000, 1000, 1000, 9000, 8000, false, 0, false, 1100),
                        plain(hit(Side.B, 2, 4), 2, 1000, 1000, 1000, 9000, 8000, false, 0, false, 1000),
                        plain(hit(Side.A, 4, 5), 2, 1000, 1000, 1000, 9000, 8000, false, 0, false, 1100)),
                log);
    }

    @Test
    void aStrongAttackerHitsHarderInCycle2Too() {
        // Adattati are strong against MEROZYN: effective ATK 4400, spill (4400 - 1500) * 1100 / 1000 = 3190
        MatchState state = Boards.match(
                Boards.side(Subfaction.ADATTATI, 12000, "2:4V(4000/3000)"),
                Boards.side(Subfaction.MEROZYN, 12000, "4:9F(1000/1500)"));

        List<MatchEvent> log = cycle2(state, List.of(hit(Side.A, 2, 4)));

        assertEquals(
                List.of(
                        plain(hit(Side.A, 2, 4), 2, 4000, 1100, 4400, 1500, -2900, true, 3190, false, 1100),
                        new CreatureDestroyed(Side.B, 4, "9F")),
                log);
        assertEquals(12000 - 3190, state.player(Side.B).life());
    }

    @Test
    void cycle2SeesTheDefLeftByCycle1() {
        // B3 (DEF 5000, no ATK) is hit by A3 in Cycle 1 (3000 -> DEF 2000), then by A1 in Cycle 2
        MatchState state = match("1:4V(4000/3000) 3:7S(3000/3000)", "3:9F(0/5000)");
        List<Attack> attacks = List.of(hit(Side.A, 1, 3), hit(Side.A, 3, 3)); // as declared: lane 1, then lane 3
        List<MatchEvent> log = new ArrayList<>();

        resolver.cycle1(state, pairAgainstHighCard, attacks, log);
        resolver.cycle2(state, pairAgainstHighCard, attacks, log);

        // spill of the Cycle 2 kill: (4000 - 2000) * 1100 / 1000 = 2200
        assertEquals(
                List.of(
                        plain(hit(Side.A, 3, 3), 1, 3000, 1000, 3000, 5000, 2000, false, 0, false, 1100),
                        plain(hit(Side.A, 1, 3), 2, 4000, 1000, 4000, 2000, -2000, true, 2200, false, 1100),
                        new CreatureDestroyed(Side.B, 3, "9F")),
                log);
        assertEquals(12000 - 2200, state.player(Side.B).life());
    }

    @Test
    void noAttacksNoEventsNoChange() {
        MatchState state = match("2:4V(4000/3000)", "4:9F(1000/1500)");

        assertEquals(List.of(), cycle2(state, List.of()));
        assertEquals(1500, Boards.creature(state.player(Side.B), 4).currentDef());
    }
}
