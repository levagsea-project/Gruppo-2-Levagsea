package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.CardDrawn;
import quorma.gameengine.log.MatchEvent.CardEffectResolved;
import quorma.gameengine.log.MatchEvent.CardMoved;
import quorma.gameengine.log.MatchEvent.CardsMilled;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.log.TargetRef;
import quorma.gameengine.model.Card;
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
 * The immediate effects that act on a player, the deck or a lane (E8, E9, E35, E36, E49, E57), as spells drawn at
 * Refill. Side A draws the spell into lane 1 (lane 2 is taken, so lanes 1, 3, 4 and 5 are empty); side B is full.
 */
class PlayerEffectsInMatchTest {

    private static final String FULL_B =
            "1:2S(1000/9000) 2:3S(1000/9000) 3:4S(1000/9000) 4:5S(1000/9000) 5:6S(1000/9000)";

    private final TurnResolver resolver = TurnResolvers.noEvaluation();
    private PlayerState a;
    private PlayerState b;
    private List<MatchEvent> log;

    private static ExpansionDef immediate(String effect, Map<String, Value> params, String target,
            Map<String, Value> targetParams) {
        return new ExpansionDef(null, Map.of(), effect, params, target, targetParams, "D_Istantanea", List.of(),
                ResClass.I);
    }

    private static SpellCard spell(String id, ExpansionDef... effects) {
        return new SpellCard(id, Faction.RESISTENZA_UMANA, List.of(new InlineEffect(List.of(effects))));
    }

    private static Value.IntV n(int v) {
        return new Value.IntV(v);
    }

    private static Value.StrV s(String v) {
        return new Value.StrV(v);
    }

    /** Side A holds {@code aDeck} (first token on top, after {@code spells}), side B is full with {@code bDeck}. */
    private void refill(String aDeck, String bDeck, SpellCard... spells) {
        a = Boards.side(Subfaction.ADATTATI, 12000, "2:9V(1000/9000)", aDeck);
        for (int i = spells.length - 1; i >= 0; i--) {
            a.deck().add(0, spells[i]);
        }
        b = Boards.side(Subfaction.SEKTAC, 12000, FULL_B, bDeck);
        MatchState state = Boards.match(a, b);
        log = new ArrayList<>();
        resolver.refill(state, log);
    }

    private static List<String> ids(List<Card> cards) {
        return Boards.ids(cards);
    }

    private List<String> lanes(PlayerState p) {
        return p.creatures().stream().map(c -> c.card().id()).toList();
    }

    // ---- E57 mill

    @Test
    void e57MillsTheTopCardsOfTheChosenPlayerWithNoEffect() {
        refill("", "4S(1000/1000) 5S(1000/1000) 6S(1000/1000)",
                spell("MILL", immediate("E57", Map.of("count", n(2)), "S30", Map.of())));

        assertEquals(List.of("4S", "5S"), ids(b.discard()));
        assertEquals(List.of("6S"), ids(b.deck()));
        assertTrue(log.contains(new CardsMilled(Side.B, "INLINE_1", List.of("4S", "5S"))));
    }

    @Test
    void e57StopsWhenTheDeckRunsOut() {
        refill("3V(1000/1000)", "", spell("MILL", immediate("E57", Map.of("count", n(5)), "S31", Map.of())));

        assertEquals(List.of("MILL", "3V"), ids(a.discard()));
        assertEquals(List.of(), ids(a.deck()));
        assertTrue(log.contains(new CardsMilled(Side.A, "INLINE_1", List.of("3V"))));
        assertEquals(List.of("9V"), lanes(a), "the milled card was the one refill would have drawn");
    }

    // ---- E35 draw first

    @Test
    void e35DrawsTheTopCreatureIntoTheLowestEmptyLaneThenRefillGoesOn() {
        refill("4V(1000/1000) 5V(1000/1000)", "", spell("DRAW", immediate("E35", Map.of(), "S31", Map.of())));

        assertEquals(List.of("4V", "9V", "5V"), lanes(a));
        assertTrue(log.contains(new CardDrawn(Side.A, 1, "4V")));
        assertTrue(log.contains(new CardDrawn(Side.A, 3, "5V")));
    }

    @Test
    void e35ResolvesASpellOnTopAtOnce() {
        refill("4V(1000/1000)", "4S(1000/1000) 5S(1000/1000)",
                spell("DRAW", immediate("E35", Map.of(), "S31", Map.of())),
                spell("MILL2", immediate("E57", Map.of("count", n(1)), "S30", Map.of())));

        assertEquals(List.of("DRAW", "MILL2"), ids(a.discard()));
        assertEquals(List.of("4S"), ids(b.discard()), "the drawn spell milled one card of the opponent");
        assertEquals(List.of("4V", "9V"), lanes(a));
    }

    @Test
    void e35LeavesACreatureOnTheDeckWhenNoLaneIsEmpty() {
        a = Boards.side(Subfaction.ADATTATI, 12000, "1:9V(1000/9000) 2:8V(1000/9000) 3:7V(1000/9000) 4:6V(1000/9000)",
                "4V(1000/1000) 5V(1000/1000)");
        a.deck().add(0, spell("DRAW", immediate("E35", Map.of(), "S31", Map.of())));
        b = Boards.side(Subfaction.SEKTAC, 12000, FULL_B);
        log = new ArrayList<>();
        resolver.refill(Boards.match(a, b), log);

        assertEquals(List.of("9V", "8V", "7V", "6V", "4V"), lanes(a));
        assertEquals(List.of("5V"), ids(a.deck()));
    }

    // ---- E49 reorder

    @Test
    void e49MovesTheHighestRankAmongTheTopCardsToTheTopOrTheBottom() {
        Map<String, Value> three = Map.of("count", n(3));
        refill("3V(1000/1000) KV(1000/1000) AV(1000/1000) 9V(1000/1000)", "",
                spell("REORDER", immediate("E49", Map.of("position", s("top")), "S47", three)));
        assertTrue(log.contains(new CardMoved(Side.A, "AV", "top")));
        assertEquals(List.of("AV", "9V", "3V", "KV", "9V"), lanes(a), "the Ace is the highest rank");

        refill("3V(1000/1000) KV(1000/1000) AV(1000/1000) 9V(1000/1000)", "",
                spell("REORDER", immediate("E49", Map.of("position", s("bottom")), "S47", three)));
        assertTrue(log.contains(new CardMoved(Side.A, "AV", "bottom")));
        assertEquals(List.of("3V", "9V", "KV", "9V", "AV"), lanes(a));
    }

    @Test
    void e49DoesNothingWhenTheTopCardsHoldNoCreature() {
        refill("3V(1000/1000)", "",
                spell("REORDER", immediate("E49", Map.of("position", s("top")), "S47", Map.of("count", n(1)))),
                spell("OTHER", immediate("E57", Map.of("count", n(0)), "S31", Map.of())));

        assertTrue(log.stream().noneMatch(e -> e instanceof CardMoved));
        assertEquals(List.of("3V", "9V"), lanes(a));
    }

    // ---- E36 return a creature

    @Test
    void e36FromTheDeckPutsTheTopCreatureInTheNamedEmptyLane() {
        refill("3V(1000/1000)", "",
                spell("BACK", immediate("E36", Map.of("source", s("deck"), "lane", n(4)), "S32", Map.of())));

        assertEquals(List.of("9V", "3V"), lanes(a));
        assertEquals(4, a.creatures().get(1).lane());
        assertTrue(log.contains(new CardDrawn(Side.A, 4, "3V")));
    }

    @Test
    void e36DoesNothingWhenTheNamedLaneIsTaken() {
        refill("3V(1000/1000)", "",
                spell("BACK", immediate("E36", Map.of("source", s("deck"), "lane", n(2)), "S32", Map.of())));

        assertEquals(List.of("3V", "9V"), lanes(a), "refill then draws the creature for lane 1 as usual");
        assertTrue(log.contains(new CardDrawn(Side.A, 1, "3V")));
    }

    // ---- E8, E9 life

    @Test
    void e8AndE9ChangeTheLifeOfTheChosenPlayerAndLogItAsACardEffect() {
        for (String effect : List.of("E8", "E9")) {
            for (Side side : Side.values()) {
                String target = side == Side.B ? "S30" : "S31";
                refill("3V(1000/1000)", "", spell("LIFE", immediate(effect, Map.of("amount", n(700)), target, Map.of())));

                int after = effect.equals("E8") ? 11300 : 12700;
                assertEquals(after, (side == Side.A ? a : b).life(), effect + " on " + target);
                assertEquals(12000, (side == Side.A ? b : a).life());
                assertTrue(log.contains(new CardEffectResolved(Side.A, "LIFE", "INLINE_1", effect,
                        new TargetRef.PlayerRef(side), List.of(new StatChange(StatChange.Stat.LIFE, 12000, after)))),
                        effect + " on " + target);
            }
        }
    }
}
