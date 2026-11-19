package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static quorma.gameengine.testkit.Attacks.hit;
import static quorma.gameengine.testkit.Evaluators.evaluation;
import static quorma.gameengine.testkit.Evaluators.hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.log.Modifier;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * Scudo, Perforante and Traboccante in combat (formulas in {@link Damage}; spill from the damage dealt, decision
 * {@code 2026-11-14-spill-after-mitigation}). Side A is Adattati, side B is SEKTAC: no advantage between them.
 * A's hand multiplier is 1100 (Pair), B's is 1000 (High card).
 * Every expected value is worked out by hand.
 */
class DamageModifiersTest {

    private static final CreatureRef A3 = new CreatureRef(Side.A, 3, "4V");
    private static final CreatureRef B3 = new CreatureRef(Side.B, 3, "9F");

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

    private static AbilityRef scudo(int x) {
        return new AbilityRef("K_SCUDO", Map.of("x", new Value.IntV(x)));
    }

    private static AbilityRef flatScudo(int x) {
        return new AbilityRef("K_SCUDO", Map.of("x", new Value.IntV(x), "kind", new Value.StrV("flat")));
    }

    private static AbilityRef perforante(int x) {
        return new AbilityRef("K_PERFORANTE", Map.of("x", new Value.IntV(x)));
    }

    private static final AbilityRef TRABOCCANTE = new AbilityRef("K_TRABOCCANTE");

    private static Modifier scudoOn(CreatureRef c, int x) {
        return new Modifier(c, "K_SCUDO", "E28", Modifier.Kind.DAMAGE_REDUCTION_PCT, x);
    }

    private static Modifier perforanteOn(CreatureRef c, int x) {
        return new Modifier(c, "K_PERFORANTE", "E56", Modifier.Kind.PIERCE_PCT, x);
    }

    private static Modifier traboccanteOn(CreatureRef c) {
        return new Modifier(c, "K_TRABOCCANTE", "E21", Modifier.Kind.SPILL_MULTIPLIER, 1200);
    }

    private static AttackResolved attack(List<MatchEvent> log) {
        return (AttackResolved) log.get(0);
    }

    @Test
    void scudoReducesTheDamageTaken() {
        MatchState state = match("3:4V(4000/3000)", "3:9F(1000/5000)");
        Boards.give(state.player(Side.B), 3, scudo(25));

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 3, 3)));

        // mitigation 750: floor(4000 * 750 / 1000) = 3000
        assertEquals(List.of(new AttackResolved(hit(Side.A, 3, 3), 1, 4000, 1000, 4000, 750, 3000, 5000, 2000,
                false, false, 1100, 1100, 0, List.of(scudoOn(B3, 25)))), log);
        assertEquals(2000, Boards.creature(state.player(Side.B), 3).currentDef());
    }

    @Test
    void perforanteAndTraboccanteOnAKillSpillFromTheDamageDealt() {
        MatchState state = match("3:4V(4000/7000)", "3:9F(1000/1700)");
        Boards.give(state.player(Side.A), 3, perforante(30), TRABOCCANTE);
        Boards.give(state.player(Side.B), 3, scudo(25));

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 3, 3)));

        // mitigation 825 (Scudo 250, Perforante leaves 175): damage 3300, DEF 1700 -> -1600, killed.
        // spill multiplier floor(1100 * 1200 / 1000) = 1320; spill floor((3300 - 1700) * 1320 / 1000) = 2112
        assertEquals(List.of(
                new AttackResolved(hit(Side.A, 3, 3), 1, 4000, 1000, 4000, 825, 3300, 1700, -1600, true, false,
                        1100, 1320, 2112, List.of(scudoOn(B3, 25), perforanteOn(A3, 30), traboccanteOn(A3))),
                new CreatureDestroyed(Side.B, 3, "9F")),
                log);
        assertEquals(12000 - 2112, state.player(Side.B).life());
    }

    @Test
    void reductionsAreCappedAtThirtyPercent() {
        // a second, inline reduction of 20%: 750 * 800 / 1000 = 600, capped at 700
        ExpansionDef twenty = new ExpansionDef("T0", Map.of(), "E28",
                Map.of("mode", new Value.StrV("reduction_pct"), "pct", new Value.IntV(20)),
                "S1", Map.of(), "D_Permanente", List.of(), ResClass.C);
        MatchState state = match("3:4V(4000/3000)", "3:9F(1000/9000)");
        Boards.give(state.player(Side.B), 3, scudo(25), new InlineEffect(List.of(twenty)));

        AttackResolved a = attack(cycle1(state, List.of(hit(Side.A, 3, 3))));

        assertEquals(700, a.mitigationPermille());
        assertEquals(2800, a.damage());
        assertEquals(List.of(scudoOn(B3, 25),
                new Modifier(B3, "INLINE_2", "E28", Modifier.Kind.DAMAGE_REDUCTION_PCT, 20)), a.modifiers());
    }

    @Test
    void flatScudoSubtractsPerBlowAndLeavesAtLeastOne() {
        MatchState state = match("3:4V(4000/3000) 4:5V(500/3000)", "3:9F(1000/9000) 4:KS(1000/9000)");
        Boards.give(state.player(Side.B), 3, flatScudo(800));
        Boards.give(state.player(Side.B), 4, flatScudo(800));

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 3, 3), hit(Side.A, 4, 4)));

        AttackResolved strong = (AttackResolved) log.get(0);
        assertEquals(3200, strong.damage(), "4000 - 800");
        assertEquals(1000, strong.mitigationPermille());
        assertEquals(List.of(new Modifier(B3, "K_SCUDO", "E28", Modifier.Kind.DAMAGE_REDUCTION_FLAT, 800)),
                strong.modifiers());
        assertEquals(1, ((AttackResolved) log.get(1)).damage(), "500 - 800 is below 0: minimum 1");
    }

    @Test
    void perforanteAlsoPiercesAFlatReduction() {
        MatchState state = match("3:4V(4000/3000)", "3:9F(1000/9000)");
        Boards.give(state.player(Side.A), 3, perforante(30));
        Boards.give(state.player(Side.B), 3, flatScudo(1000));

        // 1000 - floor(1000 * 30 / 100) = 700 left: 4000 - 700 = 3300
        assertEquals(3300, attack(cycle1(state, List.of(hit(Side.A, 3, 3)))).damage());
    }

    @Test
    void perforanteAgainstATargetWithNoReductionsChangesNothingAndIsNotLogged() {
        MatchState state = match("3:4V(4000/3000)", "3:9F(1000/9000)");
        Boards.give(state.player(Side.A), 3, perforante(30));

        AttackResolved a = attack(cycle1(state, List.of(hit(Side.A, 3, 3))));

        assertEquals(4000, a.damage());
        assertEquals(1000, a.mitigationPermille());
        assertEquals(List.of(), a.modifiers());
    }

    @Test
    void traboccanteDoesNothingWithoutAKill() {
        MatchState state = match("3:4V(4000/3000)", "3:9F(1000/9000)");
        Boards.give(state.player(Side.A), 3, TRABOCCANTE);

        AttackResolved a = attack(cycle1(state, List.of(hit(Side.A, 3, 3))));

        assertEquals(1100, a.spillMultiplierPermille(), "the hand multiplier");
        assertEquals(List.of(), a.modifiers());
    }

    @Test
    void aMutualKillHasNoSpillSoNoTraboccante() {
        MatchState state = match("3:4V(4000/1000)", "3:9F(2000/1500)");
        Boards.give(state.player(Side.A), 3, TRABOCCANTE);

        List<MatchEvent> log = cycle1(state, List.of(hit(Side.A, 3, 3), hit(Side.B, 3, 3)));

        AttackResolved a = (AttackResolved) log.get(0);
        assertEquals(true, a.mutualKill());
        assertEquals(0, a.spill());
        assertEquals(1100, a.spillMultiplierPermille());
        assertEquals(List.of(), a.modifiers());
        assertEquals(12000, state.player(Side.B).life());
    }

    @Test
    void scudoKeepsACreatureAliveThatWouldHaveDied() {
        MatchState state = match("3:4V(4000/3000)", "3:9F(1000/3500)");
        Boards.give(state.player(Side.B), 3, scudo(25));

        AttackResolved a = attack(cycle1(state, List.of(hit(Side.A, 3, 3))));

        assertEquals(false, a.killed(), "3000 damage against DEF 3500");
        assertEquals(500, a.defAfter());
        assertEquals(12000, state.player(Side.B).life());
    }

    @Test
    void anEffectiveAtkOfZeroIsNotReduced() {
        MatchState state = match("3:4V(0/3000)", "3:9F(1000/9000)");
        Boards.give(state.player(Side.B), 3, flatScudo(800));

        AttackResolved a = attack(cycle1(state, List.of(hit(Side.A, 3, 3))));

        assertEquals(0, a.damage());
        assertEquals(List.of(), a.modifiers());
    }

    @Test
    void cycle2UsesTheSameRules() {
        MatchState state = match("1:4V(4000/3000)", "2:9F(1000/1700)");
        Boards.give(state.player(Side.A), 1, perforante(30), TRABOCCANTE);
        Boards.give(state.player(Side.B), 2, scudo(25));
        List<MatchEvent> log = new ArrayList<>();

        resolver.cycle2(state, pairAgainstHighCard, List.of(hit(Side.A, 1, 2)), log);

        CreatureRef a1 = new CreatureRef(Side.A, 1, "4V");
        assertEquals(new AttackResolved(hit(Side.A, 1, 2), 2, 4000, 1000, 4000, 825, 3300, 1700, -1600, true, false,
                1100, 1320, 2112, List.of(scudoOn(new CreatureRef(Side.B, 2, "9F"), 25), perforanteOn(a1, 30),
                        traboccanteOn(a1))),
                log.get(0));
    }
}
