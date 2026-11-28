package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.log.MatchEvent.EffectResolved;
import quorma.gameengine.log.MatchEvent.EffectTriggered;
import quorma.gameengine.log.Modifier;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * The Evolutions Crescita (survives combat damage: +15% of the base ATK) and Trincea (declared as a target: +20% of
 * the base DEF), and the Spira synergies that count, scale and force them (3: +350 ATK per Evolution fired, 4: values
 * x1.3, 5: every Spira Evolution fires). An Evolution fires once per creature. Every expected value is worked out
 * by hand from {@code balance.json}.
 */
class EvolutionAndSpiraTest {

    private static final AbilityRef CRESCITA = new AbilityRef("EVO_CRESCITA");
    private static final AbilityRef TRINCEA = new AbilityRef("EVO_TRINCEA");

    private final TurnResolver resolver = new TurnResolver(TurnResolvers.RULES);

    private static MatchState match(PlayerState a, String boardB) {
        return Boards.match(a, Boards.side(Subfaction.SEKTAC, 12000, boardB));
    }

    private static List<EffectTriggered> triggered(List<MatchEvent> log) {
        return log.stream().filter(e -> e instanceof EffectTriggered).map(e -> (EffectTriggered) e).toList();
    }

    private static List<EffectResolved> resolved(List<MatchEvent> log) {
        return log.stream().filter(e -> e instanceof EffectResolved).map(e -> (EffectResolved) e).toList();
    }

    @Test
    void crescitaFiresOnceWhenTheCreatureSurvivesDamage() {
        // B's King hits A's lane 1 for 2000 (DEF 9000): it survives, so Crescita gives floor(1000 x 150 / 1000) = 150.
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2V(1000/9000)");
        Boards.give(a, 1, CRESCITA);
        MatchState state = match(a, "1:KS(2000/90000)");
        Evaluation evaluation = resolver.evaluate(state);
        List<Attack> attacks = resolver.declare(state, evaluation);

        List<MatchEvent> first = new ArrayList<>();
        resolver.cycle1(state, evaluation, attacks, first);
        assertEquals(1150, Boards.creature(a, 1).currentAtk());
        assertEquals("T12", triggered(first).get(0).triggerId());
        assertEquals("EVO_CRESCITA", triggered(first).get(0).abilityId());
        assertEquals(List.of(new StatChange(StatChange.Stat.ATK, 1000, 1150)), resolved(first).get(0).changes());

        // The same blow again: an Evolution fires at most once per creature.
        List<MatchEvent> second = new ArrayList<>();
        resolver.cycle1(state, evaluation, attacks, second);
        assertEquals(1150, Boards.creature(a, 1).currentAtk());
        assertEquals(List.of(), triggered(second));
    }

    @Test
    void trinceaFiresWhenTheCreatureIsDeclaredATarget() {
        // The King of B targets lane 1: DEF 5000 + floor(5000 x 200 / 1000) = 6000, once.
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2V(1000/5000)");
        Boards.give(a, 1, TRINCEA);
        MatchState state = match(a, "1:KS(100/90000)");
        Evaluation evaluation = resolver.evaluate(state);
        List<Attack> attacks = resolver.declare(state, evaluation);

        List<MatchEvent> log = new ArrayList<>();
        resolver.declareTriggers(state, attacks, log);
        assertEquals(6000, Boards.creature(a, 1).currentDef());
        assertEquals("T9", triggered(log).get(0).triggerId());
        assertEquals(List.of(new StatChange(StatChange.Stat.DEF, 5000, 6000)), resolved(log).get(0).changes());

        resolver.declareTriggers(state, attacks, new ArrayList<>());
        assertEquals(6000, Boards.creature(a, 1).currentDef(), "once per creature");
    }

    @Test
    void spira4MultipliesTheEvolutionValuesBy1_3() {
        // Four Spira: tiers 3 and 4. Crescita gives floor(1000 x floor(150 x 1300 / 1000) / 1000) = 195.
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2S(1000/9000) 2:3S(1000/9000) 3:4S(1000/9000) 4:5S(1000/9000)");
        Boards.give(a, 1, CRESCITA);
        MatchState state = match(a, "1:KS(2000/90000)");
        Evaluation evaluation = resolver.evaluate(state);
        assertEquals(List.of("SPIRA_3", "SPIRA_4"), evaluation.synergies().tiers(Side.A));

        resolver.cycle1(state, evaluation, resolver.declare(state, evaluation), new ArrayList<>());

        assertEquals(1195, Boards.creature(a, 1).currentAtk());
    }

    @Test
    void spira3GivesEverySpiraCreatureAnAtkBonusPerEvolutionThatFired() {
        // Round 1: Crescita fires on lane 1 (one Evolution fired). Round 2: four of a suit, the three strongest (lanes 1-3) attack. Each gets
        // Spira 3: +350 x 1. Lane 2 (base 1000) attacks with 1350; lane 1 has Crescita's 195 more (1000 + 195 + 350).
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:5S(1000/9000) 2:4S(1000/9000) 3:3S(1000/9000) 4:2S(1000/9000)");
        Boards.give(a, 1, CRESCITA);
        MatchState state = match(a, "1:KS(2000/90000) 2:9F(100/90000) 3:5T(100/90000) 4:JV(100/90000)");
        Evaluation round1 = resolver.evaluate(state);
        resolver.cycle1(state, round1, resolver.declare(state, round1), new ArrayList<>());
        assertEquals(1, Boards.creature(a, 1).evolutionsTriggered());

        Evaluation round2 = resolver.evaluate(state);
        List<MatchEvent> log = new ArrayList<>();
        resolver.cycle1(state, round2, resolver.declare(state, round2), log);

        List<AttackResolved> blows = log.stream().filter(e -> e instanceof AttackResolved).map(e -> (AttackResolved) e)
                .filter(e -> e.attack().attacker() == Side.A).toList();
        assertEquals(List.of(1545, 1350, 1350), blows.stream().map(AttackResolved::atk).toList());
        assertEquals(List.of(new Modifier(new CreatureRef(Side.A, 2, "4S"), "SPIRA_3", "E1", Modifier.Kind.ATK_BONUS, 350)),
                blows.get(1).modifiers());
    }

    @Test
    void spira5FiresTheEvolutionsOfEverySpiraCreatureOnce() {
        // A straight flush of Spira: tier 5 forces Crescita on lane 3 without any damage. Tier 4 is active too, so
        // the value is 195: 1000 + 195.
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000,
                "1:2S(1000/9000) 2:3S(1000/9000) 3:4S(1000/9000) 4:5S(1000/9000) 5:6S(1000/9000)");
        Boards.give(a, 3, CRESCITA);
        MatchState state = match(a, "1:KS(100/90000)");
        resolver.evaluate(state);

        List<MatchEvent> log = new ArrayList<>();
        resolver.forceEvolutions(state, log);

        assertEquals(1195, Boards.creature(a, 3).currentAtk());
        assertEquals("T5", triggered(log).get(0).triggerId(), "the log shows what forced it");
        assertEquals("EVO_CRESCITA", triggered(log).get(0).abilityId());

        List<MatchEvent> again = new ArrayList<>();
        resolver.forceEvolutions(state, again);
        assertEquals(1195, Boards.creature(a, 3).currentAtk());
        assertTrue(triggered(again).isEmpty(), "already fired");
    }
}
