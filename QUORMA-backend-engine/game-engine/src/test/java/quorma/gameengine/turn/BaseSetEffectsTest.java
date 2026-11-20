package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static quorma.gameengine.testkit.Attacks.hit;
import static quorma.gameengine.testkit.Evaluators.evaluation;
import static quorma.gameengine.testkit.Evaluators.hand;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.AttackRedirected;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.log.MatchEvent.CreatureSaved;
import quorma.gameengine.log.Modifier;
import quorma.gameengine.model.Ability;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.Faction;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.SpellCard;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * The abilities the base set brought back or added (docs/cards/card-effects.json), each on a small board worked out
 * by hand. Side A is Adattati (Resistenza Umana), side B is SEKTAC (Potenze Artificiali): no advantage between them,
 * and Potenze Artificiali refill first.
 */
class BaseSetEffectsTest {

    private final TurnResolver resolver = TurnResolvers.noEvaluation();
    private final Evaluation pairAgainstHighCard = evaluation(hand(HandType.PAIR, 1), 1100, hand(HandType.HIGH_CARD, 3), 1000);

    private static PlayerState a(String board) {
        return Boards.side(Subfaction.ADATTATI, 12000, board);
    }

    private static PlayerState b(String board) {
        return Boards.side(Subfaction.SEKTAC, 12000, board);
    }

    private static AbilityRef ref(String id, Object... nameValue) {
        Map<String, Value> params = new LinkedHashMap<>();
        for (int i = 0; i < nameValue.length; i += 2) {
            Object v = nameValue[i + 1];
            params.put((String) nameValue[i], v instanceof Integer n ? new Value.IntV(n) : new Value.StrV((String) v));
        }
        return new AbilityRef(id, params);
    }

    private List<MatchEvent> cycle1(MatchState state, Evaluation evaluation, Attack... attacks) {
        List<MatchEvent> log = new ArrayList<>();
        resolver.cycle1(state, evaluation, List.of(attacks), log);
        return log;
    }

    private List<MatchEvent> cycle2(MatchState state, Attack... attacks) {
        List<MatchEvent> log = new ArrayList<>();
        resolver.cycle2(state, pairAgainstHighCard, List.of(attacks), log);
        return log;
    }

    private static AttackResolved blow(List<MatchEvent> log) {
        return log.stream().filter(e -> e instanceof AttackResolved).map(e -> (AttackResolved) e).findFirst().orElseThrow();
    }

    @Test
    void letaleDestroysWhateverTheDefAndItsSpillReadsTheDefAt35Percent() {
        PlayerState a = a("1:AV(4000/6000)");
        Boards.give(a, 1, ref("K_LETALE"));
        PlayerState b = b("1:2S(1000/9000)");
        MatchState state = Boards.match(a, b);

        AttackResolved blow = blow(cycle1(state, pairAgainstHighCard, hit(Side.A, 1, 1)));

        assertEquals(4000, blow.damage());
        assertEquals(5000, blow.defAfter());
        assertTrue(blow.killed(), "Letale: any combat damage destroys");
        assertTrue(Boards.creature(b, 1).destroyed());
        // spill = (4000 - floor(9000 x 0.35)) x 1.1 = (4000 - 3150) x 1.1 = 935
        assertEquals(935, blow.spill());
        assertEquals(12000 - 935, b.life());
    }

    @Test
    void muroTakesTheAttacksOnItsNeighboursAndReducesDamageBy30Percent() {
        PlayerState a = a("1:AV(4000/6000)");
        PlayerState b = b("1:3S(1000/5000) 2:2T(1000/9000)");
        Boards.give(b, 2, ref("K_MURO"));
        MatchState state = Boards.match(a, b);

        List<MatchEvent> log = new ArrayList<>();
        List<Attack> attacks = resolver.declareTriggers(state, List.of(hit(Side.A, 1, 1)), log);
        assertTrue(log.stream().anyMatch(e -> e instanceof AttackRedirected r && r.to().lane() == 2 && r.abilityId().equals("K_MURO")));

        AttackResolved blow = blow(cycle1(state, pairAgainstHighCard, attacks.toArray(Attack[]::new)));
        assertEquals(700, blow.mitigationPermille());
        assertEquals(2800, blow.damage());
        assertEquals(6200, Boards.creature(b, 2).currentDef());
        assertEquals(5000, Boards.creature(b, 1).currentDef(), "the creature declared as target is not hit");
    }

    @Test
    void aBonusUntilTheEndOfTheTurnEndsThereAndAccumuloGrowsAtTheEndOfEachTurn() {
        PlayerState a = a("1:AV(4000/9000) 2:2S(1000/9000)");
        Boards.give(a, 1, ref("K_APRI_STRADA", "permille", 150));
        Boards.give(a, 2, ref("K_ACCUMULO", "permille", 60, "max_stacks", 3));
        PlayerState b = b("1:3S(1000/1000) 2:4S(1000/9000)");
        MatchState state = Boards.match(a, b);

        cycle1(state, pairAgainstHighCard, hit(Side.A, 1, 1));
        assertEquals(1150, Boards.creature(a, 2).currentAtk(), "Apri la strada: +15% of 1000 until the end of the turn");

        resolver.endOfTurn(state, pairAgainstHighCard, new ArrayList<>());
        assertEquals(1060, Boards.creature(a, 2).currentAtk(), "the bonus ended; Accumulo +6% at the end of the turn");
        for (int turn = 2; turn <= 4; turn++) {
            state.nextTurn();
            resolver.endOfTurn(state, pairAgainstHighCard, new ArrayList<>());
        }
        assertEquals(1180, Boards.creature(a, 2).currentAtk(), "at most 3 times");
    }

    @Test
    void custodeSavesOnceWithFullDefAndRefillDestroysIt() {
        PlayerState a = a("1:AV(4000/6000)");
        PlayerState b = b("1:QT(1000/3000)");
        Boards.give(b, 1, ref("K_CUSTODE"));
        MatchState state = Boards.match(a, b);

        List<MatchEvent> log = cycle1(state, pairAgainstHighCard, hit(Side.A, 1, 1));
        assertTrue(log.contains(new CreatureSaved(Side.B, 1, "QT", "K_CUSTODE")));
        assertEquals(0, blow(log).spill());
        assertEquals(3000, Boards.creature(b, 1).currentDef());
        assertFalse(Boards.creature(b, 1).destroyed());

        resolver.refill(state, new ArrayList<>());
        assertTrue(b.creatureAt(1).isEmpty(), "destroyed at the next Refill, still in its lane");
        assertEquals(List.of("QT"), Boards.ids(b.discard()));
    }

    @Test
    void ecoSendsBackAShareOfTheCombatDamageTaken() {
        PlayerState a = a("1:AV(4000/9000)");
        PlayerState b = b("2:4F(1000/9000)");
        Boards.give(b, 2, ref("K_ECO", "permille", 500));
        MatchState state = Boards.match(a, b);

        cycle2(state, hit(Side.A, 1, 2));

        assertEquals(5000, Boards.creature(b, 2).currentDef());
        assertEquals(9000 - 2000, Boards.creature(a, 1).currentDef(), "50% of the 4000 taken");
    }

    @Test
    void catenaDamagesTheEnemiesNextToTheTarget() {
        PlayerState a = a("2:9V(4000/9000)");
        Boards.give(a, 2, ref("K_CATENA", "permille", 300, "on", "hit"));
        PlayerState b = b("1:2S(1000/9000) 2:3S(1000/9000) 3:4S(1000/9000) 5:6S(1000/9000)");
        MatchState state = Boards.match(a, b);

        cycle1(state, pairAgainstHighCard, hit(Side.A, 2, 2));

        assertEquals(5000, Boards.creature(b, 2).currentDef());
        assertEquals(7800, Boards.creature(b, 1).currentDef(), "30% of the 4000 dealt");
        assertEquals(7800, Boards.creature(b, 3).currentDef());
        assertEquals(9000, Boards.creature(b, 5).currentDef(), "not next to the target");
    }

    @Test
    void lascitoDiDifesaGivesItsNeighboursDefWhenItIsDestroyed() {
        PlayerState a = a("2:9V(4000/9000)");
        PlayerState b = b("1:2S(1000/5000) 2:3S(1000/1000) 3:4S(1000/5000)");
        Boards.give(b, 2, ref("K_LASCITO_DIFESA", "permille", 200));
        MatchState state = Boards.match(a, b);

        cycle1(state, pairAgainstHighCard, hit(Side.A, 2, 2));

        assertTrue(Boards.creature(b, 2).destroyed());
        assertEquals(6000, Boards.creature(b, 1).currentDef(), "+20% of its base DEF");
        assertEquals(6000, Boards.creature(b, 3).currentDef());
    }

    @Test
    void assimilazioneGrowsWhenAnAllyIsDestroyed() {
        PlayerState a = a("1:AV(4000/9000)");
        PlayerState b = b("1:2S(1000/1000) 3:4S(1000/5000)");
        Boards.give(b, 3, ref("K_ASSIMILA", "permille", 100, "scope", "allies"));
        MatchState state = Boards.match(a, b);

        cycle1(state, pairAgainstHighCard, hit(Side.A, 1, 1));

        assertEquals(1100, Boards.creature(b, 3).currentAtk());
        assertEquals(1, Boards.creature(b, 3).stacks("K_ASSIMILA"));
    }

    @Test
    void primoColpoStopsTheBlowOfATargetItWouldDestroyInCycle1() {
        PlayerState a = a("1:JF(4000/5000)");
        Boards.give(a, 1, ref("K_PRIMO_COLPO"));
        PlayerState b = b("1:2S(3000/2000)");
        MatchState state = Boards.match(a, b);
        Evaluation bothAttack = evaluation(hand(HandType.PAIR, 1), 1100, hand(HandType.PAIR, 1), 1100);

        List<MatchEvent> log = cycle1(state, bothAttack, hit(Side.A, 1, 1), hit(Side.B, 1, 1));

        assertEquals(5000, Boards.creature(a, 1).currentDef(), "the target deals no combat damage to it");
        assertTrue(Boards.creature(b, 1).destroyed());
        AttackResolved bBlow = log.stream().filter(e -> e instanceof AttackResolved r && r.attack().attacker() == Side.B)
                .map(e -> (AttackResolved) e).findFirst().orElseThrow();
        assertEquals(0, bBlow.damage());
        assertTrue(bBlow.modifiers().stream().anyMatch(m -> m.kind() == Modifier.Kind.DAMAGE_PREVENTED
                && m.abilityId().equals("K_PRIMO_COLPO") && m.value() == 3000));
    }

    @Test
    void contaComeMakesAPairWithTheRankItCountsAs() {
        TurnResolver real = new TurnResolver(TurnResolvers.RULES);
        PlayerState a = a("1:5V(1000/1000) 2:QF(1000/1000) 3:2S(1000/1000)");
        PlayerState b = b("1:3T(1000/1000)");
        assertEquals(HandType.HIGH_CARD, real.evaluate(Boards.match(a, b)).a().type(), "without the keyword");

        Boards.give(a, 1, ref("K_CONTA_COME", "rank", "Q"));
        Evaluation evaluation = real.evaluate(Boards.match(a, b));
        assertEquals(HandType.PAIR, evaluation.a().type());
        assertEquals(List.of(1, 2), evaluation.a().attackerLanes());
    }

    @Test
    void aCreatureOutOfTheHandDoesNotFormCombinations() {
        TurnResolver real = new TurnResolver(TurnResolvers.RULES);
        PlayerState a = a("1:5V(1000/1000)");
        PlayerState b = b("1:KV(1000/1000) 2:KF(1000/1000) 3:2S(1000/1000)");
        assertEquals(HandType.PAIR, real.evaluate(Boards.match(a, b)).b().type());

        // Chi conta (written on the card): the opponent's highest-ranked creature does not count for its hand
        ExpansionDef exclude = new ExpansionDef("T4", Map.of(), "E63", Map.of(), "S39",
                Map.of("from", new Value.StrV("S10")), "D_FincheSorgenteInGioco", List.of(), ResClass.R);
        Boards.give(a, 1, new InlineEffect(List.of(exclude)));
        Evaluation evaluation = real.evaluate(Boards.match(a, b));
        assertEquals(HandType.HIGH_CARD, evaluation.b().type());
        assertEquals(List.of(2), evaluation.b().attackerLanes(), "the King in lane 1 (lowest lane) is out");
    }

    @Test
    void guscioStopsEveryHeal() {
        for (boolean shell : new boolean[] {false, true}) {
            PlayerState a = a("1:5T(1000/5000)");
            List<Ability> abilities = new ArrayList<>(List.of(ref("K_RIGENERANTE", "permille", 100)));
            if (shell) {
                abilities.add(ref("K_GUSCIO"));
            }
            Boards.give(a, 1, abilities.toArray(Ability[]::new));
            Boards.creature(a, 1).takeDamage(2000);
            MatchState state = Boards.match(a, b("1:2S(1000/1000)"));
            resolver.refill(state, new ArrayList<>());
            assertEquals(shell ? 3000 : 3500, Boards.creature(a, 1).currentDef(), shell ? "Guscio" : "Rigenerante: +10% of 5000");
        }
    }

    @Test
    void aSpellMalusLastsUntilTheNextRefillAndNonRiscrivibileIgnoresIt() {
        // Il colpo secco: the opponent's creatures lose 30% of their base ATK until the next Refill.
        ExpansionDef malus = new ExpansionDef("T0", Map.of(), "E1",
                Map.of("mode", new Value.StrV("pct_base_atk"), "permille", new Value.IntV(-300)), "S10", Map.of(),
                "D_FinoAlProssimoRifornimento", List.of(), ResClass.C);
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "2:9V(1000/9000)", "AV(1000/9000)");
        a.deck().add(0, new SpellCard("COLPO", Faction.RESISTENZA_UMANA, List.of(new InlineEffect(List.of(malus)))));
        PlayerState b = b("1:2S(1000/9000) 4:5S(1000/9000)");
        Boards.give(b, 1, ref("K_NON_RISCRIVIBILE", "scope", "self_adjacent"));
        MatchState state = Boards.match(a, b);

        resolver.refill(state, new ArrayList<>());
        assertEquals("AV", Boards.creature(a, 1).card().id(), "the spell, then the creature for the same lane");

        Board board = new Board(new AbilityBinder(TurnResolvers.RULES), state, pairAgainstHighCard, 0);
        assertEquals(List.of(), board.atkBonuses(Boards.creature(b, 1), null, null), "its ATK cannot be lowered");
        List<Modifier> malusOn4 = board.atkBonuses(Boards.creature(b, 4), null, null);
        assertEquals(1, malusOn4.size());
        assertEquals(Modifier.Kind.ATK_MALUS, malusOn4.get(0).kind());
        assertEquals(300, malusOn4.get(0).value());

        state.nextTurn();
        resolver.refill(state, new ArrayList<>());
        Board later = new Board(new AbilityBinder(TurnResolvers.RULES), state, pairAgainstHighCard, 0);
        assertEquals(List.of(), later.atkBonuses(Boards.creature(b, 4), null, null), "ended at the next Refill");
    }

    @Test
    void aSpellPutsTheLastDiscardedCreatureBackAndRefillGoesOn() {
        ExpansionDef back = new ExpansionDef(null, Map.of(), "E36", Map.of("source", new Value.StrV("discard")), "S32",
                Map.of(), "D_Istantanea", List.of(), ResClass.I);
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "2:9V(1000/9000)", "3V(1000/1000)");
        a.deck().add(0, new SpellCard("ANIMA", Faction.RESISTENZA_UMANA, List.of(new InlineEffect(List.of(back)))));
        a.discard().add(new CreatureCard("AV", 1, quorma.gameengine.hand.Suit.VINCOLO, Subfaction.ADATTATI, 1000, 9000));
        MatchState state = Boards.match(a, b("1:2S(1000/9000)"));

        List<MatchEvent> log = new ArrayList<>();
        resolver.refill(state, log);

        assertTrue(log.contains(new MatchEvent.CreatureReturned(Side.A, 1, "AV")));
        assertEquals("AV", Boards.creature(a, 1).card().id());
        assertEquals("3V", Boards.creature(a, 3).card().id(), "the card drawn after the spell goes to the next empty lane");
        assertEquals(List.of("ANIMA"), Boards.ids(a.discard()));
    }

    @Test
    void aSpellCanKeepTheOpponentsLanesEmptyForOneRefill() {
        // Assegnazione sospesa (Potenze Artificiali refill first): the opponent does not refill this time.
        ExpansionDef suspend = new ExpansionDef("T25", Map.of(), "E47", Map.of("player", new Value.StrV("avversario")),
                "S30", Map.of(), "D_Permanente", List.of(), ResClass.R, Map.of("max_triggers", new Value.IntV(1)));
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, "2:9S(1000/9000)", "AS(1000/9000)");
        b.deck().add(0, new SpellCard("SOSPESA", Faction.POTENZE_ARTIFICIALI, List.of(new InlineEffect(List.of(suspend)))));
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "2:9V(1000/9000)", "AV(1000/9000) 3V(1000/9000)");
        MatchState state = Boards.match(a, b);

        List<MatchEvent> log = new ArrayList<>();
        resolver.refill(state, log);
        assertTrue(log.contains(new MatchEvent.RefillPrevented(Side.A, 1, "SOSPESA")));
        assertTrue(a.creatureAt(1).isEmpty());
        assertEquals(2, a.deck().size(), "nothing drawn");

        state.nextTurn();
        resolver.refill(state, new ArrayList<>());
        assertEquals("AV", Boards.creature(a, 1).card().id(), "once only");
    }
}
