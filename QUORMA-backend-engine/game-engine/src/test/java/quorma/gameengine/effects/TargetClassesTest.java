package quorma.gameengine.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.Value;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;

/**
 * One test per way a target is used, without the board: the target classes and a small {@link TargetView} that holds
 * two sides of creatures. Boards are written by hand, side A on the left (lanes 1 to 5).
 */
class TargetClassesTest {

    private static Creature creature(int lane, int rank, Suit suit, int atk, int def) {
        CreatureCard card = new CreatureCard("c" + lane + suit, rank, suit, Subfaction.ADATTATI, atk, def, List.of());
        return new Creature(card, lane);
    }

    /** Two sides of creatures; selections run over everyone alive, as the board does. */
    private static final class View implements TargetView {
        final Map<Side, List<Creature>> sides = new LinkedHashMap<>();
        final Set<Creature> attackers = new HashSet<>();

        View(List<Creature> a, List<Creature> b) {
            sides.put(Side.A, a);
            sides.put(Side.B, b);
        }

        @Override public List<Creature> creatures(Side side) { return sides.get(side); }
        @Override public Optional<Creature> creatureAt(Side side, int lane) {
            return sides.get(side).stream().filter(c -> c.lane() == lane).findFirst();
        }
        @Override public List<Creature> everyone() {
            List<Creature> all = new ArrayList<>(sides.get(Side.A));
            all.addAll(sides.get(Side.B));
            return all;
        }
        @Override public boolean adjacent(Side side, Creature a, Creature b) { return Math.abs(a.lane() - b.lane()) == 1; }
        @Override public List<Creature> neighbours(Side side, Creature c) {
            return sides.get(side).stream().filter(x -> !x.destroyed() && Math.abs(x.lane() - c.lane()) == 1).toList();
        }
        @Override public boolean attacker(Side side, Creature c) { return attackers.contains(c); }
        @Override public boolean inCombination(Side side, Creature c) { return attackers.contains(c); }
        @Override public boolean filtersHold(List<String> filters, Side controller, Side side, Creature c) { return true; }
        @Override public int factionRank(Creature c) { return sides.get(Side.A).contains(c) ? 0 : 1; }
        @Override public Optional<Creature> select(String id, Map<String, Value> params, Side controller) {
            RankedTarget ranked = (RankedTarget) Targets.find(id).filter(RankedTarget.class::isInstance)
                    .orElseThrow(() -> new IllegalStateException("not a selection target: " + id));
            return everyone().stream().filter(c -> !c.destroyed()).min(ranked.order(this));
        }
    }

    private static Target target(String id) {
        return Targets.find(id).orElseThrow();
    }

    private static BoundExpansion expansion(String targetId, Map<String, Value> targetParams) {
        return EffectFixtures.expansion("E10", Map.of(), targetId, targetParams);
    }

    @Test
    void everyTargetIdOfTheEngineHasOneClass() {
        assertEquals(27, Targets.all().size());
        assertTrue(Targets.find("S99").isEmpty());
        assertEquals(6, Targets.all().stream().filter(RankedTarget.class::isInstance).count());
    }

    @Test
    void aStaticEffectReachesByTargetAndSide() {
        Creature source = creature(2, 5, Suit.VINCOLO, 1000, 3000);
        Creature ally = creature(3, 6, Suit.VINCOLO, 1000, 3000);
        Creature foeSameLane = creature(2, 7, Suit.FRATTURA, 1000, 3000);
        Creature foeNext = creature(3, 8, Suit.FRATTURA, 1000, 3000);
        Creature foeFar = creature(5, 9, Suit.FRATTURA, 1000, 3000);
        View view = new View(List.of(source, ally), List.of(foeSameLane, foeNext, foeFar));
        view.attackers.add(ally);

        assertTrue(target("S1").reaches(view, Side.A, source, Side.A, source));
        assertFalse(target("S1").reaches(view, Side.A, source, Side.A, ally));
        assertFalse(target("S1").reaches(view, Side.A, null, Side.A, source), "a spell has no source");
        assertTrue(target("S27").reaches(view, Side.A, source, Side.A, source));
        assertTrue(target("S41").reaches(view, Side.A, source, Side.A, ally));
        assertFalse(target("S41").reaches(view, Side.A, source, Side.B, foeNext), "only allies");
        assertTrue(target("S6").reaches(view, Side.A, source, Side.B, foeSameLane));
        assertFalse(target("S6").reaches(view, Side.A, source, Side.B, foeNext));
        assertTrue(target("S45").reaches(view, Side.A, source, Side.B, foeNext));
        assertFalse(target("S45").reaches(view, Side.A, source, Side.B, foeFar));
        assertFalse(target("S45").reaches(view, Side.A, source, Side.B, foeSameLane), "the lane facing the source is not reached");
        assertTrue(target("S9").reaches(view, Side.A, source, Side.A, ally));
        assertFalse(target("S9").reaches(view, Side.A, source, Side.B, foeNext));
        assertTrue(target("S10").reaches(view, Side.A, source, Side.B, foeNext));
        assertTrue(target("S11").reaches(view, Side.A, source, Side.B, foeFar));
        assertTrue(target("S17").reaches(view, Side.A, source, Side.A, ally));
        assertFalse(target("S17").reaches(view, Side.A, source, Side.A, source), "does not attack");
        assertTrue(target("S20").reaches(view, Side.A, source, Side.A, ally));
        assertFalse(target("S33").reaches(view, Side.A, source, Side.A, source), "a ranked target is not a static one");
    }

    @Test
    void aTriggeredEffectChoosesItsCreaturesWhenItResolves() {
        Creature source = creature(2, 5, Suit.VINCOLO, 1000, 3000);
        Creature left = creature(1, 6, Suit.SPIRA, 1000, 3000);
        Creature right = creature(3, 7, Suit.VINCOLO, 1000, 3000);
        Creature foeSameLane = creature(2, 8, Suit.FRATTURA, 1000, 3000);
        Creature foeOther = creature(4, 9, Suit.FRATTURA, 1000, 3000);
        foeOther.markDestroyed();
        View view = new View(List.of(left, source, right), List.of(foeSameLane, foeOther));
        BoundExpansion e = expansion("S1", Map.of());

        assertEquals(List.of(source), target("S1").select(view, Side.A, source, TriggerContext.NONE, e));
        assertEquals(List.of(foeSameLane), target("S6").select(view, Side.A, source, TriggerContext.NONE, e));
        assertEquals(List.of(left), target("S7").select(view, Side.A, source, TriggerContext.NONE, e),
                "the adjacent ally in the lowest lane");
        assertEquals(List.of(left, source, right), target("S9").select(view, Side.A, source, TriggerContext.NONE, e));
        assertEquals(List.of(foeSameLane), target("S10").select(view, Side.A, source, TriggerContext.NONE, e),
                "a destroyed creature is not reached");
        assertEquals(List.of(left, source, right, foeSameLane),
                target("S11").select(view, Side.A, source, TriggerContext.NONE, e));
        assertEquals(List.of(left, right), target("S41").select(view, Side.A, source, TriggerContext.NONE, e));
        assertEquals(List.of(left, right),
                target("S44").select(view, Side.A, source, TriggerContext.dealt(Side.A, source, 100, false), e));
        assertEquals(List.of(), target("S44").select(view, Side.A, source, TriggerContext.NONE, e));
        assertEquals(List.of(source, right), target("S12").select(view, Side.A, source, TriggerContext.NONE,
                expansion("S12", Map.of("suit", new Value.StrV("Vincolo")))));
        assertEquals(List.of(source), target("S2").select(view, Side.A, source, TriggerContext.triggeredBy(Side.A, source), e));
        assertEquals(List.of(), target("S2").select(view, Side.A, source, TriggerContext.NONE, e));
        assertEquals("target S4 has no selection for a triggered effect",
                assertThrows(IllegalStateException.class,
                        () -> target("S4").select(view, Side.A, source, TriggerContext.NONE, e)).getMessage());
    }

    @Test
    void anImmediateEffectAimsAtTheReceiverGroupsOrAPickAndRemembersIt() {
        Creature a1 = creature(1, 5, Suit.VINCOLO, 1000, 3000);
        Creature a2 = creature(2, 6, Suit.VINCOLO, 4000, 3000);
        Creature b1 = creature(1, 7, Suit.FRATTURA, 1000, 2000);
        View view = new View(List.of(a1, a2), List.of(b1));
        BoundExpansion e = expansion("S1", Map.of());
        Map<String, Creature> chosen = new LinkedHashMap<>();

        assertEquals(List.of(a2), target("S27").selectImmediate(view, e, Side.A, a2, chosen));
        assertEquals(List.of(), target("S27").selectImmediate(view, e, Side.A, null, chosen));
        assertEquals(List.of(a1, a2), target("S9").selectImmediate(view, e, Side.A, null, chosen));
        assertEquals(List.of(b1), target("S10").selectImmediate(view, e, Side.A, null, chosen));
        assertEquals(List.of(a1, a2, b1), target("S11").selectImmediate(view, e, Side.A, null, chosen));
        assertEquals(Map.of(), chosen, "groups choose nothing");

        assertEquals(List.of(a2), target("S35").selectImmediate(view, e, Side.A, null, chosen));
        assertEquals(Map.of("S35", a2), chosen);
        assertEquals(List.of(a1), target("S33").selectImmediate(view, e, Side.A, null, chosen),
                "lanes tie at 1, side A comes first");
        assertEquals(a1, chosen.get("S33"));

        Map<String, Value> of = Map.of("of", new Value.StrV("S35"));
        assertEquals(List.of(a1), target("S44").selectImmediate(view, expansion("S44", of), Side.A, null, chosen),
                "the neighbours of the creature S35 chose");
        assertEquals(List.of(), target("S44").selectImmediate(view, expansion("S44", of), Side.A, null, new LinkedHashMap<>()));
    }

    @Test
    void rankedTargetsPickByTheirOwnOrderWithTiesToLowestLaneThenFaction() {
        Creature highAtk = creature(2, 5, Suit.VINCOLO, 5000, 1000);
        Creature highDef = creature(3, 6, Suit.SPIRA, 1000, 6000);
        Creature ace = creature(4, 1, Suit.VESTIGIO, 2000, 3000);
        Creature low = creature(5, 2, Suit.FRATTURA, 1500, 500);
        View view = new View(List.of(highAtk, highDef), List.of(ace, low));
        Map<String, Value> none = Map.of();

        assertEquals(Optional.of(highAtk), view.select("S33", none, Side.A), "lowest lane");
        assertEquals(Optional.of(highAtk), view.select("S35", none, Side.A));
        assertEquals(Optional.of(highDef), view.select("S37", none, Side.A));
        assertEquals(Optional.of(low), view.select("S38", none, Side.A));
        assertEquals(Optional.of(ace), view.select("S39", none, Side.A), "the Ace is the highest rank");
        assertEquals(Optional.of(low), view.select("S40", none, Side.A), "rank 2 is the lowest");
        assertEquals("not a selection target: S9",
                assertThrows(IllegalStateException.class, () -> view.select("S9", none, Side.A)).getMessage());
    }

    @Test
    void aPlayersAbilityAndAHandEffectPickTheirCreatures() {
        Creature source = creature(2, 5, Suit.VINCOLO, 1000, 3000);
        Creature other = creature(3, 6, Suit.VINCOLO, 1000, 3000);
        Creature dead = creature(4, 7, Suit.VINCOLO, 1000, 3000);
        dead.markDestroyed();
        Creature foe = creature(2, 8, Suit.FRATTURA, 1000, 3000);
        View view = new View(List.of(source, other, dead), List.of(foe));
        BoundExpansion e = expansion("S12", Map.of("suit", new Value.StrV("Vincolo")));

        assertEquals(List.of(source, other), target("S12").ofPlayerAbility(view, Side.A, null, e));
        assertEquals(List.of(source), target("S1").ofPlayerAbility(view, Side.A, source, e));
        assertEquals(List.of(), target("S1").ofPlayerAbility(view, Side.A, null, e));
        assertEquals(List.of(), target("S9").ofPlayerAbility(view, Side.A, source, e));

        assertEquals(Optional.of(source), target("S1").pick(view, e, Side.A, source));
        assertEquals(Optional.empty(), target("S1").pick(view, e, Side.A, null));
        assertEquals(Optional.of(foe), target("S6").pick(view, e, Side.A, source));
        assertEquals(Optional.empty(), target("S6").pick(view, e, Side.A, null));
        assertEquals(Optional.of(source), target("S33").pick(view, e, Side.A, null),
                "a ranked target picks by ranking: lane 2 ties, side A first");
    }

    @Test
    void onlyTheSingleCreatureTargetsFizzleAsTargetDestroyed() {
        for (String id : List.of("S1", "S2", "S26", "S27", "S43", "S6", "S7")) {
            assertTrue(target(id).single(), id);
        }
        for (String id : List.of("S9", "S10", "S11", "S12", "S41", "S44", "S35")) {
            assertFalse(target(id).single(), id);
        }
    }

    @Test
    void targetParamsAreCheckedPerTarget() {
        Map<String, Value> suit = Map.of("suit", new Value.StrV("Spira"));
        Map<String, Value> filters = Map.of("filters", new Value.ListV(List.of(new Value.StrV("enemy"))));
        assertTrue(target("S9").acceptsParams(Map.of()));
        assertTrue(target("S9").acceptsParams(filters));
        assertTrue(target("S33").acceptsParams(Map.of("from", new Value.StrV("S10"))));
        assertFalse(target("S33").acceptsParams(Map.of("from", new Value.StrV("S1"))));
        assertFalse(target("S9").acceptsParams(Map.of("filters", new Value.ListV(List.of(new Value.StrV("nope"))))));
        assertFalse(target("S9").acceptsParams(suit), "suit belongs to S12");
        assertTrue(target("S12").acceptsParams(suit));
        assertFalse(target("S12").acceptsParams(Map.of()), "S12 needs a suit");
        assertFalse(target("S12").acceptsParams(Map.of("suit", new Value.StrV("Cuori"))));
        assertTrue(target("S44").acceptsParams(Map.of("of", new Value.StrV("S35"))));
        assertTrue(target("S44").acceptsParams(Map.of()));
        assertFalse(target("S44").acceptsParams(Map.of("of", new Value.StrV("S9"))));
        assertTrue(target("S47").acceptsParams(Map.of("count", new Value.IntV(3))));
        assertFalse(target("S47").acceptsParams(Map.of()), "S47 needs a count");
        assertFalse(target("S47").acceptsParams(Map.of("count", new Value.IntV(0))));
    }
}
