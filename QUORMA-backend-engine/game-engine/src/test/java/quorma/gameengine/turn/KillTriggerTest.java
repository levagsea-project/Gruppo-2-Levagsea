package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static quorma.gameengine.testkit.Attacks.hit;
import static quorma.gameengine.testkit.Blows.plain;
import static quorma.gameengine.testkit.Evaluators.evaluation;
import static quorma.gameengine.testkit.Evaluators.hand;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.FizzleReason;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.log.MatchEvent.EffectFizzled;
import quorma.gameengine.log.MatchEvent.EffectResolved;
import quorma.gameengine.log.MatchEvent.EffectTriggered;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * Kills trigger T19 (Insaziabile) and the stack resolves: once every Cycle 1 blow has landed, and after each
 * Cycle 2 attack (decision {@code 2026-11-14-stack-resolves-when-quiet}). Side A is Adattati (Resistenza Umana),
 * side B is SEKTAC (Potenze Artificiali), no advantage between them. Insaziabile: +150 per mille of base ATK.
 */
class KillTriggerTest {

    private static final AbilityRef INSAZIABILE = new AbilityRef("K_INSAZIABILE");

    private final TurnResolver resolver = TurnResolvers.noEvaluation();
    private final Evaluation pairAgainstHighCard = evaluation(hand(HandType.PAIR, 3), 1100, hand(HandType.HIGH_CARD, 3), 1000);

    private static MatchState match(String boardA, String boardB) {
        return Boards.match(
                Boards.side(Subfaction.ADATTATI, 12000, boardA), Boards.side(Subfaction.SEKTAC, 12000, boardB));
    }

    private List<MatchEvent> cycle1(MatchState state, List<Attack> attacks) {
        List<MatchEvent> log = new ArrayList<>();
        resolver.cycle1(state, pairAgainstHighCard, attacks, log);
        return log;
    }

    private List<MatchEvent> cycle2(MatchState state, List<Attack> attacks) {
        List<MatchEvent> log = new ArrayList<>();
        resolver.cycle2(state, pairAgainstHighCard, attacks, log);
        return log;
    }

    private static List<MatchEvent> effects(List<MatchEvent> log) {
        return log.stream().filter(e -> e instanceof EffectTriggered || e instanceof EffectResolved).toList();
    }

    private static EffectTriggered triggered(Side side, Creature c, int stackSize) {
        return new EffectTriggered(CreatureRef.of(side, c), "K_INSAZIABILE", "T19", "E1", stackSize);
    }

    private static EffectResolved grew(Side side, Creature c, int before, int after, int stacks) {
        CreatureRef ref = CreatureRef.of(side, c);
        return new EffectResolved(ref, "K_INSAZIABILE", "E1", ref,
                List.of(new StatChange(StatChange.Stat.ATK, before, after)), OptionalInt.of(stacks));
    }

    @Test
    void aCycle1KillGrowsTheKillerAfterTheBlowAndItsSpill() {
        MatchState state = match("3:4V(4000/3000)", "3:9F(1000/1500)");
        Creature killer = Boards.give(state.player(Side.A), 3, INSAZIABILE);

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 3, 3)));

        // the blow and its spill use ATK 4000: (4000 - 1500) * 1100 / 1000 = 2750; then 4000 + 600
        assertEquals(List.of(
                plain(hit(Side.A, 3, 3), 1, 4000, 1000, 4000, 1500, -2500, true, 2750, false, 1100),
                new CreatureDestroyed(Side.B, 3, "9F"),
                triggered(Side.A, killer, 1),
                grew(Side.A, killer, 4000, 4600, 1)),
                log);
        assertEquals(4600, killer.currentAtk());
    }

    @Test
    void noKillNoTrigger() {
        MatchState state = match("3:4V(1000/3000)", "3:9F(1000/3000)");
        Boards.give(state.player(Side.A), 3, INSAZIABILE);
        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 3, 3)));
        assertEquals(1, log.size(), "only the attack: " + log);
    }

    @Test
    void aKillerDestroyedInTheSameExchangeTriggersButFizzles() {
        MatchState state = match("3:4V(4000/1000)", "3:9F(2000/1500)");
        Creature killer = Boards.give(state.player(Side.A), 3, INSAZIABILE);

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 3, 3), hit(Side.B, 3, 3)));

        assertEquals(List.of(
                triggered(Side.A, killer, 1),
                new EffectFizzled(CreatureRef.of(Side.A, killer), "K_INSAZIABILE", "E1", FizzleReason.TARGET_DESTROYED)),
                log.subList(log.size() - 2, log.size()));
        assertEquals(4000, killer.currentAtk());
    }

    @Test
    void simultaneousKillsArePushedPotenzeArtificialiFirstSoResistenzaResolvesFirst() {
        MatchState state = match("1:4V(4000/5000) 2:5V(1000/500)", "1:9F(1000/1000) 2:KS(3000/5000)");
        Creature a1 = Boards.give(state.player(Side.A), 1, INSAZIABILE);
        Creature b2 = Boards.give(state.player(Side.B), 2, INSAZIABILE);

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 1, 1), hit(Side.B, 2, 2)));

        assertEquals(List.of(
                triggered(Side.B, b2, 1),
                triggered(Side.A, a1, 2),
                grew(Side.A, a1, 4000, 4600, 1),
                grew(Side.B, b2, 3000, 3450, 1)), // 3000 * 150 / 1000 = 450
                effects(log));
    }

    @Test
    void sameSideKillsArePushedInLaneOrder() {
        MatchState state = match("1:4V(4000/5000) 2:5V(2000/5000)", "1:9F(1000/1000) 2:KS(1000/1000)");
        Creature a1 = Boards.give(state.player(Side.A), 1, INSAZIABILE);
        Creature a2 = Boards.give(state.player(Side.A), 2, INSAZIABILE);

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 1, 1), hit(Side.A, 2, 2)));

        assertEquals(List.of(
                triggered(Side.A, a1, 1),
                triggered(Side.A, a2, 2),
                grew(Side.A, a2, 2000, 2300, 1),
                grew(Side.A, a1, 4000, 4600, 1)),
                effects(log));
    }

    @Test
    void aCycle2KillResolvesBeforeTheNextAttack() {
        MatchState state = match("1:4V(4000/3000) 4:5V(1000/5000)", "2:9F(1000/1000) 3:KS(1000/5000)");
        Creature killer = Boards.give(state.player(Side.A), 1, INSAZIABILE);

        List<MatchEvent> log = cycle2(state, List.of(hit(Side.A, 1, 2), hit(Side.B, 3, 4)));

        assertEquals(List.of(AttackResolved.class, CreatureDestroyed.class, EffectTriggered.class,
                        EffectResolved.class, AttackResolved.class),
                log.stream().map(Object::getClass).toList());
        assertEquals(grew(Side.A, killer, 4000, 4600, 1), log.get(3));
    }
}
