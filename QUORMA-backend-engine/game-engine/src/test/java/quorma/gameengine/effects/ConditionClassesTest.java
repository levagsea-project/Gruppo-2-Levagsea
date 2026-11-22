package quorma.gameengine.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.Value;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/**
 * One focused test per condition class, without the board: the condition, a fake {@link ConditionContext} that answers
 * from fields, and its params as a card writes them.
 */
class ConditionClassesTest {

    /** Answers from fields, so each test sets only what its condition reads. */
    private static final class FakeView implements ConditionContext {
        boolean attacker;
        int suitCount;
        boolean firstCycle;
        Optional<HandType> handType = Optional.empty();
        boolean adjacentOfSuit;
        boolean wouldKill;
        boolean defAboveAtk;
        boolean locked;
        boolean vacated;
        int allies;
        TriggerContext trigger = TriggerContext.NONE;
        boolean wasAdjacent;

        @Override public boolean attacker(Side side, Creature creature) { return attacker; }
        @Override public int suitCount(Side side, Suit suit) { return suitCount; }
        @Override public boolean firstCycle() { return firstCycle; }
        @Override public Optional<HandType> handType(Side side) { return handType; }
        @Override public boolean hasAdjacentOfSuit(Side side, Creature source, Suit suit) { return adjacentOfSuit; }
        @Override public boolean wouldKill() { return wouldKill; }
        @Override public boolean targetDefAboveAtkAtCycleStart(Creature source) { return defAboveAtk; }
        @Override public boolean locked(Side side, String key) { return locked; }
        @Override public boolean vacatedSuit(Side side, int lane, Suit suit) { return vacated; }
        @Override public int adjacentAllyCount(Side side, Creature creature) { return allies; }
        @Override public TriggerContext trigger() { return trigger; }
        @Override public boolean wasAdjacent(Side side, Creature source, Creature destroyed, int lane) { return wasAdjacent; }
    }

    private static final Creature SOURCE = EffectFixtures.creature(1000, 3000);

    private static ConditionDef def(String id, Map<String, Value> params) {
        return new ConditionDef(id, params);
    }

    private static boolean holds(ConditionDef def, FakeView view, Creature source) {
        return Conditions.find(def.id()).orElseThrow().holds(view, def, Side.A, source);
    }

    private static Optional<String> problem(ConditionDef def) {
        return Conditions.find(def.id()).orElseThrow().validate(def);
    }

    @Test
    void everyIdOfTheEngineHasOneClass() {
        assertEquals(13, Conditions.all().size());
        assertTrue(Conditions.find("C99").isEmpty());
    }

    @Test
    void c1AndC2NeedASourceAndReadTheAttackers() {
        FakeView view = new FakeView();
        view.attacker = true;
        assertTrue(holds(def("C1", Map.of()), view, SOURCE));
        assertFalse(holds(def("C2", Map.of()), view, SOURCE));
        view.attacker = false;
        assertFalse(holds(def("C1", Map.of()), view, SOURCE));
        assertTrue(holds(def("C2", Map.of()), view, SOURCE));
        assertFalse(holds(def("C1", Map.of()), view, null), "an ability of the player has no attacker");
        assertFalse(holds(def("C2", Map.of()), view, null));
    }

    @Test
    void c4NeedsAtLeastLevelCreaturesOfTheSuit() {
        ConditionDef c4 = def("C4", Map.of("suit", new Value.StrV("Spira"), "level", new Value.IntV(3)));
        FakeView view = new FakeView();
        view.suitCount = 2;
        assertFalse(holds(c4, view, SOURCE));
        view.suitCount = 3;
        assertTrue(holds(c4, view, SOURCE));
    }

    @Test
    void theSuitAndLevelParamsAreChecked() {
        assertEquals("condition C4 needs a param 'suit' (one of [Frattura, Spira, Vestigio, Vincolo])",
                problem(def("C4", Map.of("level", new Value.IntV(3)))).orElseThrow());
        assertEquals("condition C4 needs an integer param 'level' between 1 and 5",
                problem(def("C4", Map.of("suit", new Value.StrV("Spira"), "level", new Value.IntV(6)))).orElseThrow());
        assertEquals(Optional.empty(), problem(def("C11", Map.of("suit", new Value.StrV("Vincolo")))));
        assertTrue(problem(def("C25", Map.of("suit", new Value.StrV("Cuori")))).isPresent());
    }

    @Test
    void c6HoldsOnlyInTheFirstCycle() {
        FakeView view = new FakeView();
        assertFalse(holds(def("C6", Map.of()), view, SOURCE));
        view.firstCycle = true;
        assertTrue(holds(def("C6", Map.of()), view, SOURCE));
    }

    @Test
    void c9AcceptsOneNameOrAListAndNeedsAnEvaluatedHand() {
        ConditionDef one = def("C9", Map.of("hand_type", new Value.StrV("Coppia")));
        ConditionDef list = def("C9", Map.of("hand_type", new Value.ListV(java.util.List.of(
                new Value.StrV("Coppia"), new Value.StrV("Tris")))));
        FakeView view = new FakeView();
        assertFalse(holds(one, view, SOURCE), "no hand yet");
        view.handType = Optional.of(HandType.THREE_OF_A_KIND);
        assertFalse(holds(one, view, SOURCE));
        assertTrue(holds(list, view, SOURCE));
        assertTrue(problem(def("C9", Map.of("hand_type", new Value.StrV("Cinquina")))).isPresent());
        assertTrue(problem(def("C9", Map.of())).isPresent());
    }

    @Test
    void c11C25NeedASource() {
        FakeView view = new FakeView();
        view.adjacentOfSuit = true;
        view.vacated = true;
        ConditionDef c11 = def("C11", Map.of("suit", new Value.StrV("Vincolo")));
        ConditionDef c25 = def("C25", Map.of("suit", new Value.StrV("Vincolo")));
        assertTrue(holds(c11, view, SOURCE));
        assertTrue(holds(c25, view, SOURCE));
        assertFalse(holds(c11, view, null));
        assertFalse(holds(c25, view, null));
    }

    @Test
    void c21ReadsTheBlowAndC22NeedsASource() {
        FakeView view = new FakeView();
        view.wouldKill = true;
        view.defAboveAtk = true;
        assertTrue(holds(def("C21", Map.of()), view, SOURCE));
        assertTrue(holds(def("C22", Map.of()), view, SOURCE));
        assertFalse(holds(def("C22", Map.of()), view, null));
    }

    @Test
    void c24HoldsWhileTheLockoutIsNotActive() {
        ConditionDef c24 = def("C24", Map.of("key", new Value.StrV("custode")));
        FakeView view = new FakeView();
        assertTrue(holds(c24, view, SOURCE));
        view.locked = true;
        assertFalse(holds(c24, view, SOURCE));
        assertEquals("condition C24 needs a text param 'key'", problem(def("C24", Map.of())).orElseThrow());
    }

    @Test
    void c26CountsAdjacentAlliesBetweenMinAndMaxWithDefaultsZeroAndTwo() {
        FakeView view = new FakeView();
        view.allies = 1;
        ConditionDef atLeastTwo = def("C26", Map.of("min", new Value.IntV(2)));
        ConditionDef atMostOne = def("C26", Map.of("max", new Value.IntV(1)));
        assertFalse(holds(atLeastTwo, view, SOURCE));
        assertTrue(holds(atMostOne, view, SOURCE));
        assertTrue(holds(def("C26", Map.of()), view, SOURCE));
        assertFalse(holds(def("C26", Map.of()), view, null));
        assertEquals("condition C26 takes integer params 'min' and 'max' between 0 and 2",
                problem(def("C26", Map.of("min", new Value.IntV(3)))).orElseThrow());
    }

    @Test
    void c27ReadsTheTriggerAndC28NeedsTheDestroyedCreature() {
        FakeView view = new FakeView();
        assertFalse(holds(def("C27", Map.of()), view, SOURCE));
        view.trigger = TriggerContext.dealt(Side.B, SOURCE, 100, true);
        assertTrue(holds(def("C27", Map.of()), view, SOURCE));

        view.wasAdjacent = true;
        assertFalse(holds(def("C28", Map.of()), view, SOURCE), "no creature was destroyed");
        view.trigger = TriggerContext.destroyed(SOURCE, null, null);
        assertTrue(holds(def("C28", Map.of()), view, SOURCE));
        assertFalse(holds(def("C28", Map.of()), view, null));
    }
}
