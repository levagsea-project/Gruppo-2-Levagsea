package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.Value;
import quorma.gameengine.effects.AbilityException;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.CardDrawn;
import quorma.gameengine.log.MatchEvent.CreatureDiscarded;
import quorma.gameengine.log.MatchEvent.EquipmentApplied;
import quorma.gameengine.log.MatchEvent.EquipmentLost;
import quorma.gameengine.log.MatchEvent.EquipmentPending;
import quorma.gameengine.log.MatchEvent.LaneLeftEmpty;
import quorma.gameengine.log.MatchEvent.SpellResolved;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.SpellCard;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.Evaluators;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * Refill (ruleset 10): destroyed creatures go to the discard pile, then every empty lane draws the next
 * creature from the top of the deck (index 0), in lane order 1 to 5. A player with no cards leaves the lane empty.
 * Potenze Artificiali go first, then Resistenza Umana, and with the same faction player A:
 * all the discards first in that order, then all the draws, each player finishing all its lanes before the other
 * starts (10.3: "Ogni giocatore pesca ... in ordine di corsia. Le Potenze Artificiali pescano per prime").
 *
 * <p>A is Adattati (Resistenza Umana), B is SEKTAC (Potenze Artificiali) unless a test says otherwise.
 *
 * <p>Spells and equipment drawn in Refill (10.5, 10.6) are tested with a recording {@link CardEffects}: the engine
 * logs the event and calls the hook, and a spell or equipment goes to the discard pile after its use (10.7).
 */
class RefillTest {

    private final TurnResolver resolver = TurnResolvers.noEvaluation();

    /** Five creatures 2V, 3V, 4V, 5V, 6V in lanes 1 to 5; the lanes in {@code destroyed} are flagged destroyed. */
    private static String board(int... destroyed) {
        String[] ids = {"2V", "3V", "4V", "5V", "6V"};
        StringBuilder sb = new StringBuilder();
        for (int lane = 1; lane <= 5; lane++) {
            boolean dead = false;
            for (int d : destroyed) {
                dead |= d == lane;
            }
            sb.append(lane).append(':').append(ids[lane - 1]).append("(1000/1000)").append(dead ? "!" : "").append(' ');
        }
        return sb.toString();
    }

    private List<MatchEvent> refill(MatchState state) {
        List<MatchEvent> log = new ArrayList<>();
        resolver.refill(state, log);
        return log;
    }

    private static MatchState match(PlayerState a, PlayerState b) {
        return Boards.match(a, b);
    }

    @Test
    void nothingHappensWhenEveryLaneHoldsALiveCreature() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(), "KV(1/1)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board(), "QF(1/1)");

        assertEquals(List.of(), refill(match(a, b)));
        assertEquals(List.of("KV"), Boards.ids(a.deck()));
        assertEquals(List.of("QF"), Boards.ids(b.deck()));
    }

    @Test
    void aDestroyedCreatureIsDiscardedAndReplacedByTheTopOfTheDeck() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(2), "KV(4000/6000) QF(3000/2500)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board());

        List<MatchEvent> log = refill(match(a, b));

        assertEquals(List.of(new CreatureDiscarded(Side.A, 2, "3V"), new CardDrawn(Side.A, 2, "KV")), log);
        assertEquals(List.of("3V"), Boards.ids(a.discard()));
        assertEquals(List.of("QF"), Boards.ids(a.deck()), "index 0 was the top and has been drawn");
        Creature drawn = Boards.creature(a, 2);
        assertEquals("KV", drawn.card().id());
        assertEquals(6000, drawn.currentDef());
        assertFalse(drawn.destroyed());
        assertEquals(2, drawn.lane());
    }

    @Test
    void aDrawnCreatureEntersPlayWithItsAbilitiesBound() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(2));
        a.deck().add(new CreatureCard("KV", 13, Suit.VINCOLO, Subfaction.ADATTATI, 4000, 6000,
                List.of(new AbilityRef("K_SCUDO", Map.of("x", new Value.IntV(25))), new AbilityRef("K_INSAZIABILE"))));
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board());

        refill(match(a, b));

        Creature drawn = Boards.creature(a, 2);
        assertEquals(List.of("E28", "E1"), drawn.abilities().stream().map(BoundExpansion::effectId).toList());
    }

    @Test
    void drawingACreatureWithAnAbilityTheEngineCannotPlayFails() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(2));
        a.deck().add(new CreatureCard("KV", 13, Suit.VINCOLO, Subfaction.ADATTATI, 4000, 6000,
                List.of(new AbilityRef("K_LETALE"))));
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board());

        assertThrows(AbilityException.class, () -> refill(match(a, b)));
    }

    @Test
    void aCreatureIsDiscardedEvenThoughItsDefWasNeverReducedToZero() {
        // flagged destroyed with DEF 1000: the flag decides
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(2), "KV(4000/6000)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board());

        refill(match(a, b));

        assertEquals(List.of("3V"), Boards.ids(a.discard()));
    }

    @Test
    void emptyLanesAreFilledInLaneOrderFromTheTopOfTheDeck() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(2, 4), "KV(1/1) QF(1/1) 3S(1/1)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board());

        List<MatchEvent> log = refill(match(a, b));

        assertEquals(
                List.of(
                        new CreatureDiscarded(Side.A, 2, "3V"),
                        new CreatureDiscarded(Side.A, 4, "5V"),
                        new CardDrawn(Side.A, 2, "KV"),
                        new CardDrawn(Side.A, 4, "QF")),
                log);
        assertEquals(List.of("3V", "5V"), Boards.ids(a.discard()));
        assertEquals(List.of("3S"), Boards.ids(a.deck()));
    }

    @Test
    void anEmptyDeckLeavesTheLaneEmpty() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(2));
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board());

        List<MatchEvent> log = refill(match(a, b));

        assertEquals(List.of(new CreatureDiscarded(Side.A, 2, "3V"), new LaneLeftEmpty(Side.A, 2)), log);
        assertTrue(a.creatureAt(2).isEmpty());
        assertEquals(List.of("3V"), Boards.ids(a.discard()));
    }

    @Test
    void aDeckThatRunsOutInTheMiddleFillsTheFirstLanesOnly() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(2, 4), "KV(1/1)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board());

        List<MatchEvent> log = refill(match(a, b));

        assertEquals(
                List.of(
                        new CreatureDiscarded(Side.A, 2, "3V"),
                        new CreatureDiscarded(Side.A, 4, "5V"),
                        new CardDrawn(Side.A, 2, "KV"),
                        new LaneLeftEmpty(Side.A, 4)),
                log);
        assertEquals("KV", Boards.creature(a, 2).card().id());
        assertTrue(a.creatureAt(4).isEmpty());
        assertTrue(a.deck().isEmpty());
    }

    @Test
    void laneThatWereAlreadyEmptyAreFilledToo() {
        // ruleset 10.3: "i sostituti per le proprie corsie vuote"; lanes 2 to 5 were empty before this Refill
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2V(1000/1000)", "KV(1/1) QF(1/1)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board());

        List<MatchEvent> log = refill(match(a, b));

        assertEquals(
                List.of(
                        new CardDrawn(Side.A, 2, "KV"),
                        new CardDrawn(Side.A, 3, "QF"),
                        new LaneLeftEmpty(Side.A, 4),
                        new LaneLeftEmpty(Side.A, 5)),
                log);
    }

    @Test
    void survivorsKeepTheirDamage() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(2), "KV(1/1)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board());
        Creature survivor = Boards.creature(a, 1);
        survivor.takeDamage(400);

        refill(match(a, b));

        assertSame(survivor, Boards.creature(a, 1), "a creature that survived stays where it is");
        assertEquals(600, survivor.currentDef());
    }

    @Test
    void potenzeArtificialiDiscardAndDrawFirstThenResistenzaUmana() {
        // both sides lose lanes 1 and 2; B (Potenze Artificiali) finishes all its lanes before A starts
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(1, 2), "KV(1/1) QF(1/1)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board(1, 2), "AV(1/1) 2F(1/1)");

        List<MatchEvent> log = refill(match(a, b));

        assertEquals(
                List.of(
                        new CreatureDiscarded(Side.B, 1, "2V"),
                        new CreatureDiscarded(Side.B, 2, "3V"),
                        new CreatureDiscarded(Side.A, 1, "2V"),
                        new CreatureDiscarded(Side.A, 2, "3V"),
                        new CardDrawn(Side.B, 1, "AV"),
                        new CardDrawn(Side.B, 2, "2F"),
                        new CardDrawn(Side.A, 1, "KV"),
                        new CardDrawn(Side.A, 2, "QF")),
                log);
    }

    @Test
    void withTheSameFactionPlayerAGoesFirst() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(1), "KV(1/1)");
        PlayerState b = Boards.side(Subfaction.PORTANOME, 12000, board(1), "QF(1/1)"); // also Resistenza Umana

        List<MatchEvent> log = refill(match(a, b));

        assertEquals(
                List.of(
                        new CreatureDiscarded(Side.A, 1, "2V"),
                        new CreatureDiscarded(Side.B, 1, "2V"),
                        new CardDrawn(Side.A, 1, "KV"),
                        new CardDrawn(Side.B, 1, "QF")),
                log);
    }

    @Test
    void oneEmptyDeckDoesNotStopTheOtherPlayer() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(1)); // no cards
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board(1), "AV(1/1)");

        List<MatchEvent> log = refill(match(a, b));

        assertEquals(
                List.of(
                        new CreatureDiscarded(Side.B, 1, "2V"),
                        new CreatureDiscarded(Side.A, 1, "2V"),
                        new CardDrawn(Side.B, 1, "AV"),
                        new LaneLeftEmpty(Side.A, 1)),
                log);
    }

    // ---- spells and equipment (10.5 to 10.8) ----

    /** Remembers every hook call, and what the owner's discard pile held at that moment. */
    private static final class RecordingEffects implements CardEffects {
        final List<String> calls = new ArrayList<>();
        final List<List<String>> discardAtCall = new ArrayList<>();
        final PlayerState owner;

        RecordingEffects(PlayerState owner) {
            this.owner = owner;
        }

        @Override
        public void onSpell(SpellCard card, Side side) {
            calls.add("spell " + card.id() + " " + side);
            discardAtCall.add(Boards.ids(owner.discard()));
        }

        @Override
        public void onEquipment(EquipmentCard card, Side side, Creature receiver) {
            calls.add("equipment " + card.id() + " " + side + " " + receiver.card().id() + "@" + receiver.lane());
            discardAtCall.add(Boards.ids(owner.discard()));
        }
    }

    private static List<MatchEvent> refill(MatchState state, CardEffects effects) {
        TurnResolver resolver = new TurnResolver(TurnResolvers.RULES, cards -> {
            throw new AssertionError("Refill must not evaluate hands");
        }, Evaluators.byType(), effects);
        List<MatchEvent> log = new ArrayList<>();
        resolver.refill(state, log);
        return log;
    }

    @Test
    void aDrawnSpellTakesEffectAtOnceIsDiscardedAndAnotherCardIsDrawn() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(2), "spell:S1 KV(1/1)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board());
        RecordingEffects effects = new RecordingEffects(a);
        List<MatchEvent> log = refill(match(a, b), effects);
        assertEquals(
                List.of(
                        new CreatureDiscarded(Side.A, 2, "3V"),
                        new SpellResolved(Side.A, 2, "S1"),
                        new CardDrawn(Side.A, 2, "KV")),
                log);
        assertEquals(List.of("spell S1 A"), effects.calls);
        assertEquals(List.of("3V", "S1"), Boards.ids(a.discard()));
        assertEquals("KV", Boards.creature(a, 2).card().id());
        assertTrue(a.deck().isEmpty());
    }

    @Test
    void anEquipmentIsAppliedToTheNextCreatureDrawnAndDiscardedAfterIt() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(2), "equip:E1 KV(1/1)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board());
        RecordingEffects effects = new RecordingEffects(a);
        List<MatchEvent> log = refill(match(a, b), effects);
        assertEquals(
                List.of(
                        new CreatureDiscarded(Side.A, 2, "3V"),
                        new EquipmentPending(Side.A, "E1"),
                        new CardDrawn(Side.A, 2, "KV"),
                        new EquipmentApplied(Side.A, 2, "E1", "KV")),
                log);
        assertEquals(List.of("equipment E1 A KV@2"), effects.calls);
        assertEquals(List.of(List.of("3V")), effects.discardAtCall, "the equipment is still in use when the hook runs");
        assertEquals(List.of("3V", "E1"), Boards.ids(a.discard()));
    }

    @Test
    void anEquipmentWithNoCreatureToReceiveItIsLostAndDiscarded() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(2), "equip:E1");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board());
        RecordingEffects effects = new RecordingEffects(a);
        List<MatchEvent> log = refill(match(a, b), effects);
        assertEquals(
                List.of(
                        new CreatureDiscarded(Side.A, 2, "3V"),
                        new EquipmentPending(Side.A, "E1"),
                        new LaneLeftEmpty(Side.A, 2),
                        new EquipmentLost(Side.A, "E1")),
                log);
        assertEquals(List.of(), effects.calls);
        assertEquals(List.of("3V", "E1"), Boards.ids(a.discard()));
    }

    @Test
    void aWaitingEquipmentSurvivesASpellDrawnBeforeTheCreature() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(2), "equip:E1 spell:S1 KV(1/1)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board());
        RecordingEffects effects = new RecordingEffects(a);
        List<MatchEvent> log = refill(match(a, b), effects);
        assertEquals(
                List.of(
                        new CreatureDiscarded(Side.A, 2, "3V"),
                        new EquipmentPending(Side.A, "E1"),
                        new SpellResolved(Side.A, 2, "S1"),
                        new CardDrawn(Side.A, 2, "KV"),
                        new EquipmentApplied(Side.A, 2, "E1", "KV")),
                log);
        assertEquals(List.of("spell S1 A", "equipment E1 A KV@2"), effects.calls);
        assertEquals(List.of("3V", "S1", "E1"), Boards.ids(a.discard()));
    }

    @Test
    void anEquipmentAppliesOnlyToTheNextCreatureNotToTheOnesAfterIt() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, board(2, 4), "equip:E1 KV(1/1) QF(1/1)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, board());
        RecordingEffects effects = new RecordingEffects(a);
        refill(match(a, b), effects);
        assertEquals(List.of("equipment E1 A KV@2"), effects.calls);
    }
}
