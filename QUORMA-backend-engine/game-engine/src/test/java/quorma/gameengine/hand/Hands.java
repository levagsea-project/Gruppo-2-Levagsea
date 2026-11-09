package quorma.gameengine.hand;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

/**
 * Test helper: write a board the way a player reads it.
 *
 * <pre>
 *   Hands.hand("1:AV 2:KF 3:10S 4:QT 5:2F")   // explicit lanes
 *   Hands.hand("AV KF 10S QT 2F")              // no lanes: lane = position, 1..5
 *   Hands.hand("2:9V 5:9F")                    // gaps are fine: lanes 2 and 5 only
 *   Hands.hand("")                             // empty board
 * </pre>
 *
 * Token = {@code [lane:]RankSuit}. Rank: {@code A 2..10 J Q K} (Ace is 1). Suit: {@code V} Vincolo,
 * {@code F} Frattura, {@code S} Spira, {@code T} Vestigio (V is taken, T as in "vesTigio"). Case-insensitive.
 * Values outside the {@link HandCard} limits (lane 1..5, rank 1..13) are rejected. A Five of a kind is five
 * cards with the same rank, for example {@code "9V 9F 9S 9T 9V"}.
 */
final class Hands {

    private Hands() {}

    /** Parses a board. Throws {@link IllegalArgumentException} on a malformed token. */
    static List<HandCard> hand(String spec) {
        List<HandCard> cards = new ArrayList<>();
        String trimmed = spec.trim();
        if (trimmed.isEmpty()) {
            return cards;
        }
        String[] tokens = trimmed.split("\\s+");
        for (int i = 0; i < tokens.length; i++) {
            cards.add(card(tokens[i], i + 1));
        }
        return cards;
    }

    private static HandCard card(String token, int defaultLane) {
        String body = token;
        int lane = defaultLane;
        int colon = token.indexOf(':');
        if (colon >= 0) {
            lane = Integer.parseInt(token.substring(0, colon));
            body = token.substring(colon + 1);
        }
        if (body.length() < 2) {
            throw new IllegalArgumentException("bad card '" + token + "': expected RankSuit, e.g. 10S");
        }
        String rank = body.substring(0, body.length() - 1).toUpperCase();
        char suit = Character.toUpperCase(body.charAt(body.length() - 1));
        return new HandCard(lane, rank(rank, token), suit(suit, token));
    }

    private static int rank(String s, String token) {
        return switch (s) {
            case "A" -> 1;
            case "J" -> 11;
            case "Q" -> 12;
            case "K" -> 13;
            default -> {
                try {
                    yield Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("bad rank in '" + token + "'");
                }
            }
        };
    }

    private static Suit suit(char c, String token) {
        return switch (c) {
            case 'V' -> Suit.VINCOLO;
            case 'F' -> Suit.FRATTURA;
            case 'S' -> Suit.SPIRA;
            case 'T' -> Suit.VESTIGIO;
            default -> throw new IllegalArgumentException("bad suit in '" + token + "': use V, F, S or T");
        };
    }

    /** Builds an expected result: type, attacker lanes (ascending) and tiebreak values (Ace = 14). */
    static HandResult result(HandType type, List<Integer> attackerLanes, Integer... tiebreak) {
        return new HandResult(type, attackerLanes, List.of(tiebreak));
    }

    /** Asserts the type, the attacker lanes and the tiebreak values in one go. */
    static void assertResult(HandResult actual, HandType type, List<Integer> attackerLanes, Integer... tiebreak) {
        assertEquals(result(type, attackerLanes, tiebreak), actual);
    }

    /** Every ordering of the cards, in a fixed order, for determinism tests (5 cards give 120). */
    static List<List<HandCard>> permutations(List<HandCard> cards) {
        List<List<HandCard>> out = new ArrayList<>();
        permute(new ArrayList<>(cards), 0, out);
        return out;
    }

    private static void permute(List<HandCard> items, int start, List<List<HandCard>> out) {
        if (start == items.size()) {
            out.add(new ArrayList<>(items));
            return;
        }
        for (int i = start; i < items.size(); i++) {
            java.util.Collections.swap(items, start, i);
            permute(items, start + 1, out);
            java.util.Collections.swap(items, start, i);
        }
    }
}
