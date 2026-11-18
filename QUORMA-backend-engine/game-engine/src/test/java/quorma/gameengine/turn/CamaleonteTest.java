package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/** Camaleonte (E55) in the real Evaluate phase: the creature takes the suit that gives the best hand. */
class CamaleonteTest {

    private static final AbilityRef CAMALEONTE = new AbilityRef("K_CAMALEONTE", Map.of());

    private static TurnResolver real() {
        return new TurnResolver(TurnResolvers.RULES);
    }

    @Test
    void aCamaleonteCompletesAFlush() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:AV(1000/1000) 2:3V(1000/1000) 3:5V(1000/1000) 4:7V(1000/1000) 5:9F(1000/1000)");
        Boards.give(a, 5, CAMALEONTE);
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, "");

        assertEquals(HandType.FLUSH, real().evaluate(Boards.match(a, b)).a().type());
    }

    @Test
    void withoutCamaleonteTheSameBoardIsOnlyFourOfASuit() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:AV(1000/1000) 2:3V(1000/1000) 3:5V(1000/1000) 4:7V(1000/1000) 5:9F(1000/1000)");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, "");

        assertEquals(HandType.FOUR_OF_A_SUIT, real().evaluate(Boards.match(a, b)).a().type());
    }

    @Test
    void camaleonteIsAWildSuitOnlyForTheCreatureThatHasIt() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:AV(1000/1000) 2:3V(1000/1000)");
        Boards.give(a, 2, CAMALEONTE);
        assertFalse(Boards.creature(a, 1).wildSuit());
        assertTrue(Boards.creature(a, 2).wildSuit());
    }
}
