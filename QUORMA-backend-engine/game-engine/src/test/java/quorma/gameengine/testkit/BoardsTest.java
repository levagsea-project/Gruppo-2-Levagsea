package quorma.gameengine.testkit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.Faction;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.SpellCard;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.model.Target;

/** The helpers are test infrastructure for everyone, so they are tested too. */
class BoardsTest {

    @Test
    void explicitLanesAndStats() {
        PlayerState p = Boards.side(Subfaction.ADATTATI, 12000, "1:4V(4000/3000) 3:9F(1000/1500)");
        assertEquals(12000, p.life());
        assertEquals(Faction.RESISTENZA_UMANA, p.faction());
        assertEquals(2, p.creatures().size());
        CreatureCard c = Boards.creature(p, 1).card();
        assertEquals(new CreatureCard("4V", 4, Suit.VINCOLO, Subfaction.ADATTATI, 4000, 3000), c);
        assertEquals(new CreatureCard("9F", 9, Suit.FRATTURA, Subfaction.ADATTATI, 1000, 1500), Boards.creature(p, 3).card());
        assertTrue(p.creatureAt(2).isEmpty());
    }

    @Test
    void lanesDefaultToPosition() {
        PlayerState p = Boards.side(Subfaction.SEKTAC, 1, "AV(1/1) KF(2/2) 10S(3/3)");
        assertEquals("AV", Boards.creature(p, 1).card().id());
        assertEquals(1, Boards.creature(p, 1).card().rank());
        assertEquals(13, Boards.creature(p, 2).card().rank());
        assertEquals("10S", Boards.creature(p, 3).card().id());
        assertEquals(Suit.SPIRA, Boards.creature(p, 3).card().suit());
    }

    @Test
    void emptyBoardAndEmptyDeck() {
        PlayerState p = Boards.side(Subfaction.SEKTAC, 500, "");
        assertTrue(p.creatures().isEmpty());
        assertTrue(p.deck().isEmpty());
        assertEquals(Faction.POTENZE_ARTIFICIALI, p.faction());
    }

    @Test
    void destroyedFlagAndSubfactionOverride() {
        PlayerState p = Boards.side(Subfaction.SEKTAC, 1, "1:2T(1000/1000)! 2:3V(1000/1000)@MEROZYN");
        assertTrue(Boards.creature(p, 1).destroyed());
        assertEquals(1000, Boards.creature(p, 1).currentDef(), "the flag does not change DEF");
        assertFalse(Boards.creature(p, 2).destroyed());
        assertEquals(Subfaction.MEROZYN, Boards.creature(p, 2).card().subfaction());
        assertEquals(Subfaction.SEKTAC, Boards.creature(p, 1).card().subfaction());
    }

    @Test
    void deckKeepsOrderAndUnderstandsSpellsAndEquipment() {
        PlayerState p = Boards.side(Subfaction.ADATTATI, 1, "", "KS(2000/2600) spell:S1 equip:E1 2F(1000/1000)@SEGNATERRA");
        List<Card> deck = p.deck();
        assertEquals(List.of("KS", "S1", "E1", "2F"), Boards.ids(deck));
        assertEquals(new SpellCard("S1", Faction.RESISTENZA_UMANA), deck.get(1));
        assertEquals(new EquipmentCard("E1", Faction.RESISTENZA_UMANA), deck.get(2));
        assertEquals(Subfaction.SEGNATERRA, ((CreatureCard) deck.get(3)).subfaction());
    }

    @Test
    void malformedTokensFail() {
        assertThrows(IllegalArgumentException.class, () -> Boards.side(Subfaction.SEKTAC, 1, "4V"));            // no stats
        assertThrows(IllegalArgumentException.class, () -> Boards.side(Subfaction.SEKTAC, 1, "4X(1/1)"));       // bad suit
        assertThrows(IllegalArgumentException.class, () -> Boards.side(Subfaction.SEKTAC, 1, "1:ZV(1/1)"));     // bad rank
        assertThrows(IllegalArgumentException.class, () -> Boards.side(Subfaction.SEKTAC, 1, "6:4V(1/1)"));     // lane out of range
        assertThrows(IllegalArgumentException.class, () -> Boards.side(Subfaction.SEKTAC, 1, "", "4V"));        // deck token without stats
        assertThrows(IllegalArgumentException.class, () -> Boards.side(Subfaction.SEKTAC, 1, "1:4V(0/0)"));     // DEF below 1
    }

    @Test
    void twoCreaturesInOneLaneFail() {
        assertThrows(IllegalStateException.class, () -> Boards.side(Subfaction.SEKTAC, 1, "1:4V(1/1) 1:5V(1/1)"));
    }

    @Test
    void subfactionOfAnotherFactionFails() {
        assertThrows(IllegalArgumentException.class, () -> Boards.side(Subfaction.SEKTAC, 1, "1:4V(1/1)@ADATTATI"));
    }

    @Test
    void matchHelpers() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 10, "");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 20, "");
        assertEquals(1, Boards.match(a, b).turn());
        assertEquals(50, Boards.match(a, b, 50).turn());
        assertEquals(20, Boards.match(a, b).player(Side.B).life());
    }

    @Test
    void attackHelpers() {
        assertEquals(
                new Attack(Side.A, 2, Optional.of(new Target(Side.B, 4))), Attacks.hit(Side.A, 2, 4));
        assertEquals(new Attack(Side.B, 3, Optional.empty()), Attacks.noTarget(Side.B, 3));
        assertTrue(Attacks.hit(Side.A, 3, 3).inCycle1());
        assertFalse(Attacks.hit(Side.A, 3, 4).inCycle1());
    }

    @Test
    void evaluatorHelpers() {
        HandResult pair = Evaluators.hand(HandType.PAIR, 2, 4);
        assertEquals(new HandResult(HandType.PAIR, List.of(2, 4), List.of()), pair);

        var seq = Evaluators.sequence(pair, HandResult.none());
        assertEquals(pair, seq.apply(List.of()));
        assertEquals(HandResult.none(), seq.apply(List.of()));
        assertEquals(pair, seq.apply(List.of()), "the sequence starts again");

        var cmp = Evaluators.byType();
        assertTrue(cmp.compare(pair, Evaluators.hand(HandType.HIGH_CARD, 1)) > 0);
        assertTrue(cmp.compare(HandResult.none(), pair) < 0);
        assertEquals(0, cmp.compare(pair, Evaluators.hand(HandType.PAIR, 3)));

        var recording = new Evaluators.Recording(cards -> pair);
        recording.apply(List.of());
        assertEquals(1, recording.calls().size());
    }
}
