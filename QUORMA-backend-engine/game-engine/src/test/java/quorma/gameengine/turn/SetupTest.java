package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.CardDrawn;
import quorma.gameengine.log.MatchEvent.CardSetAside;
import quorma.gameengine.log.MatchEvent.DeckReshuffled;
import quorma.gameengine.log.MatchEvent.LaneLeftEmpty;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.rng.Shuffle;
import quorma.gameengine.rng.SplitMix64;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * Setup (ruleset 5, decision {@code 2026-11-14-match-setup}). Decks are given already shuffled: the first token is
 * the top. Expected reshuffles use a fresh PRNG with the same seed, consumed in the order the decision fixes.
 */
class SetupTest {

    private static final AbilityBinder BINDER = new AbilityBinder(TurnResolvers.RULES);
    private static final long SEED = 7;

    private final List<MatchEvent> log = new ArrayList<>();

    private static MatchState match(String deckA, String deckB) {
        return Boards.match(Boards.side(Subfaction.ADATTATI, 12000, "", deckA),
                Boards.side(Subfaction.SEKTAC, 12000, "", deckB));
    }

    private static List<String> ids(List<Card> cards) {
        return cards.stream().map(Card::id).toList();
    }

    @Test
    void aFullBoardFromCreaturesOnlyNeedsNoReshuffle() {
        MatchState state = match("2V(1/1) 3V(1/1) 4V(1/1) 5V(1/1) 6V(1/1) 7V(1/1)", "2S(1/1) 3S(1/1) 4S(1/1) 5S(1/1) 6S(1/1)");
        SplitMix64 rng = new SplitMix64(SEED);

        Setup.run(state, BINDER, rng, log);

        assertEquals(List.of(
                new CardDrawn(Side.A, 1, "2V"), new CardDrawn(Side.A, 2, "3V"), new CardDrawn(Side.A, 3, "4V"),
                new CardDrawn(Side.A, 4, "5V"), new CardDrawn(Side.A, 5, "6V"),
                new CardDrawn(Side.B, 1, "2S"), new CardDrawn(Side.B, 2, "3S"), new CardDrawn(Side.B, 3, "4S"),
                new CardDrawn(Side.B, 4, "5S"), new CardDrawn(Side.B, 5, "6S")),
                log);
        assertEquals(List.of("7V"), ids(state.player(Side.A).deck()), "the rest of the deck, untouched");
        assertEquals(new SplitMix64(SEED).nextLong(), rng.nextLong(), "nothing set aside: the PRNG is not used");
    }

    @Test
    void spellsAndEquipmentAreSetAsideThenShuffledBackPlayerAFirst() {
        MatchState state = match("2V(1/1) spell:SA 3V(1/1) 4V(1/1) 5V(1/1) equip:EA 6V(1/1) 7V(1/1)",
                "spell:SB 2S(1/1) 3S(1/1) 4S(1/1) 5S(1/1) 6S(1/1) 7S(1/1)");

        Setup.run(state, BINDER, new SplitMix64(SEED), log);

        // A: rest of the deck [7V], then SA and EA in the order they were set aside, shuffled first; then B.
        SplitMix64 expected = new SplitMix64(SEED);
        List<String> orderA = Shuffle.shuffled(List.of("7V", "SA", "EA"), expected);
        List<String> orderB = Shuffle.shuffled(List.of("7S", "SB"), expected);
        assertEquals(List.of(
                new CardDrawn(Side.A, 1, "2V"), new CardSetAside(Side.A, "SA"), new CardDrawn(Side.A, 2, "3V"),
                new CardDrawn(Side.A, 3, "4V"), new CardDrawn(Side.A, 4, "5V"), new CardSetAside(Side.A, "EA"),
                new CardDrawn(Side.A, 5, "6V"),
                new CardSetAside(Side.B, "SB"), new CardDrawn(Side.B, 1, "2S"), new CardDrawn(Side.B, 2, "3S"),
                new CardDrawn(Side.B, 3, "4S"), new CardDrawn(Side.B, 4, "5S"), new CardDrawn(Side.B, 5, "6S"),
                new DeckReshuffled(Side.A, 2, orderA),
                new DeckReshuffled(Side.B, 1, orderB)),
                log);
        assertEquals(orderA, ids(state.player(Side.A).deck()));
        assertEquals(orderB, ids(state.player(Side.B).deck()));
    }

    @Test
    void anEmptyDeckLeavesTheRemainingLanesEmpty() {
        MatchState state = match("2V(1/1) spell:SA 3V(1/1)", "spell:SB");

        Setup.run(state, BINDER, new SplitMix64(SEED), log);

        assertEquals(List.of(
                new CardDrawn(Side.A, 1, "2V"), new CardSetAside(Side.A, "SA"), new CardDrawn(Side.A, 2, "3V"),
                new LaneLeftEmpty(Side.A, 3), new LaneLeftEmpty(Side.A, 4), new LaneLeftEmpty(Side.A, 5),
                new CardSetAside(Side.B, "SB"), new LaneLeftEmpty(Side.B, 1), new LaneLeftEmpty(Side.B, 2),
                new LaneLeftEmpty(Side.B, 3), new LaneLeftEmpty(Side.B, 4), new LaneLeftEmpty(Side.B, 5),
                new DeckReshuffled(Side.A, 1, List.of("SA")),
                new DeckReshuffled(Side.B, 1, List.of("SB"))),
                log);
        assertEquals(List.of(), state.player(Side.B).creatures());
    }

    @Test
    void placedCreaturesHaveTheirAbilitiesBound() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2V(1000/1000)");
        Boards.give(a, 1, new AbilityRef("K_INSAZIABILE"));
        CreatureCard insaziabile = Boards.creature(a, 1).card();
        MatchState state = Boards.match(new PlayerState(a.faction(), 12000, List.of(insaziabile)),
                Boards.side(Subfaction.SEKTAC, 12000, "", "2S(1/1)"));

        Setup.run(state, BINDER, new SplitMix64(SEED), log);

        assertEquals("K_INSAZIABILE", Boards.creature(state.player(Side.A), 1).abilities().get(0).abilityId());
    }
}
