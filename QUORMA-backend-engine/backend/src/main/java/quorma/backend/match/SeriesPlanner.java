package quorma.backend.match;

import java.util.List;
import quorma.backend.catalogue.Faction;

/**
 * The rules of a series (2026-11-19-best-of-three-faction-swap), with no database and no engine. Best of three: a win
 * is 1 point, a draw half a point each. Game 3 is played unless one player already has 2 points after game 2.
 * Points are counted in halves, so a draw is 1 and a win is 2.
 */
public final class SeriesPlanner {

    public static final int MAX_GAMES = 3;

    private static final int HALVES_TO_WIN = 4;

    private SeriesPlanner() {
    }

    /** True when another game must be played, given the outcomes of the games played so far (seen from player A). */
    public static boolean needsNextGame(List<MatchOutcome> played) {
        if (played.size() >= MAX_GAMES) {
            return false;
        }
        if (played.size() < 2) {
            return true;
        }
        return halves(played, true) < HALVES_TO_WIN && halves(played, false) < HALVES_TO_WIN;
    }

    /** The outcome of a finished series: more points wins, equal points is a draw. */
    public static MatchOutcome seriesOutcome(List<MatchOutcome> played) {
        if (needsNextGame(played)) {
            throw new IllegalStateException("the series is not finished: " + played.size() + " games played");
        }
        int a = halves(played, true);
        int b = halves(played, false);
        return a > b ? MatchOutcome.A_WIN : a < b ? MatchOutcome.B_WIN : MatchOutcome.DRAW;
    }

    /**
     * The faction player A plays in a game: the chosen one in game 1, the opposite in game 2, the chosen one again in
     * game 3. Player B always has the other faction.
     */
    public static Faction factionOfA(Faction firstGame, int gameNumber) {
        if (gameNumber < 1 || gameNumber > MAX_GAMES) {
            throw new IllegalArgumentException("game number out of range: " + gameNumber);
        }
        return gameNumber == 2 ? firstGame.opposite() : firstGame;
    }

    private static int halves(List<MatchOutcome> played, boolean forA) {
        int total = 0;
        for (MatchOutcome o : played) {
            total += switch (o) {
                case A_WIN -> forA ? 2 : 0;
                case B_WIN -> forA ? 0 : 2;
                case DRAW -> 1;
            };
        }
        return total;
    }
}
