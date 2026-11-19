package quorma.backend.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static quorma.backend.match.MatchOutcome.A_WIN;
import static quorma.backend.match.MatchOutcome.B_WIN;
import static quorma.backend.match.MatchOutcome.DRAW;

import java.util.List;
import org.junit.jupiter.api.Test;
import quorma.backend.catalogue.Faction;

class SeriesPlannerTest {

    @Test
    void firstTwoGamesAreAlwaysPlayed() {
        assertTrue(SeriesPlanner.needsNextGame(List.of()));
        assertTrue(SeriesPlanner.needsNextGame(List.of(A_WIN)));
        assertTrue(SeriesPlanner.needsNextGame(List.of(DRAW)));
    }

    @Test
    void twoToZeroEndsTheSeriesForEitherPlayer() {
        assertFalse(SeriesPlanner.needsNextGame(List.of(A_WIN, A_WIN)));
        assertFalse(SeriesPlanner.needsNextGame(List.of(B_WIN, B_WIN)));
        assertEquals(MatchOutcome.A_WIN, SeriesPlanner.seriesOutcome(List.of(A_WIN, A_WIN)));
        assertEquals(MatchOutcome.B_WIN, SeriesPlanner.seriesOutcome(List.of(B_WIN, B_WIN)));
    }

    @Test
    void everyOtherScoreAfterTwoGamesPlaysTheThird() {
        assertTrue(SeriesPlanner.needsNextGame(List.of(A_WIN, B_WIN))); // 1 - 1
        assertTrue(SeriesPlanner.needsNextGame(List.of(DRAW, DRAW))); // 1 - 1
        assertTrue(SeriesPlanner.needsNextGame(List.of(A_WIN, DRAW))); // 1.5 - 0.5
        assertTrue(SeriesPlanner.needsNextGame(List.of(DRAW, A_WIN))); // 1.5 - 0.5
        assertTrue(SeriesPlanner.needsNextGame(List.of(B_WIN, DRAW))); // 0.5 - 1.5
        assertTrue(SeriesPlanner.needsNextGame(List.of(DRAW, B_WIN))); // 0.5 - 1.5
    }

    @Test
    void nothingIsPlayedAfterTheThirdGame() {
        assertFalse(SeriesPlanner.needsNextGame(List.of(A_WIN, B_WIN, A_WIN)));
        assertFalse(SeriesPlanner.needsNextGame(List.of(DRAW, DRAW, DRAW)));
    }

    @Test
    void morePointsWinsTheSeries() {
        assertEquals(MatchOutcome.A_WIN, SeriesPlanner.seriesOutcome(List.of(A_WIN, B_WIN, A_WIN)));
        assertEquals(MatchOutcome.B_WIN, SeriesPlanner.seriesOutcome(List.of(A_WIN, B_WIN, B_WIN)));
        assertEquals(MatchOutcome.A_WIN, SeriesPlanner.seriesOutcome(List.of(A_WIN, DRAW, DRAW))); // 2 - 1
        assertEquals(MatchOutcome.A_WIN, SeriesPlanner.seriesOutcome(List.of(DRAW, DRAW, A_WIN))); // 2 - 1
        assertEquals(MatchOutcome.B_WIN, SeriesPlanner.seriesOutcome(List.of(B_WIN, DRAW, DRAW)));
    }

    @Test
    void equalPointsAtTheEndAreADraw() {
        assertEquals(MatchOutcome.DRAW, SeriesPlanner.seriesOutcome(List.of(DRAW, DRAW, DRAW)));
        assertEquals(MatchOutcome.DRAW, SeriesPlanner.seriesOutcome(List.of(A_WIN, B_WIN, DRAW))); // 1.5 - 1.5
        assertEquals(MatchOutcome.DRAW, SeriesPlanner.seriesOutcome(List.of(A_WIN, DRAW, B_WIN))); // 1.5 - 1.5
        assertEquals(MatchOutcome.DRAW, SeriesPlanner.seriesOutcome(List.of(DRAW, B_WIN, A_WIN))); // 1.5 - 1.5
    }

    @Test
    void anUnfinishedSeriesHasNoOutcome() {
        assertThrows(IllegalStateException.class, () -> SeriesPlanner.seriesOutcome(List.of(A_WIN, B_WIN)));
    }

    @Test
    void factionsSwapInGameTwoAndComeBackInGameThree() {
        assertEquals(Faction.RESISTENZA_UMANA, SeriesPlanner.factionOfA(Faction.RESISTENZA_UMANA, 1));
        assertEquals(Faction.POTENZE_ARTIFICIALI, SeriesPlanner.factionOfA(Faction.RESISTENZA_UMANA, 2));
        assertEquals(Faction.RESISTENZA_UMANA, SeriesPlanner.factionOfA(Faction.RESISTENZA_UMANA, 3));
        assertEquals(Faction.RESISTENZA_UMANA, SeriesPlanner.factionOfA(Faction.POTENZE_ARTIFICIALI, 2));
        assertThrows(IllegalArgumentException.class, () -> SeriesPlanner.factionOfA(Faction.RESISTENZA_UMANA, 4));
    }
}
