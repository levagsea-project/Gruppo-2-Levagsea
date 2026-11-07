package quorma.gameengine.hand;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import quorma.gameengine.data.Ruleset;

/**
 * Poker hand evaluation for the Valutazione phase.<br>
 *
 * Evaluates poker-like hands, compares results and reads their spill multipliers.
 */
public final class HandEvaluator {

    private HandEvaluator() {}

    /**
     * Finds the best hand formed by the creatures in play and the creatures that attack.
     *
     * <p>Input is 0 to 5 cards with distinct lanes. An empty list gives {@link HandResult#none()}.
     * When the lowest rank is tied in a Four of a suit, the first matching card in the input is excluded.
     *
     * @throws IllegalArgumentException if there are more than 5 cards or two cards share a lane
     */
    public static HandResult evaluate(List<HandCard> creatures) {
        // Check limits and handle an empty board.
        if (creatures.size() > 5) {
            throw new IllegalArgumentException("Cannot evaluate more than 5 cards, got " + creatures.size());
        }
        if (creatures.isEmpty()) return HandResult.none();
 
        // Check duplicate lanes, build maps of ranks and suits.
        boolean[] lanes = new boolean[HandCard.MAX_LANE + 1];
        Map<Integer, List<HandCard>> ranks = new HashMap<>();
        Map<Suit, List<HandCard>> suits = new EnumMap<>(Suit.class);
        for (HandCard card : creatures) {
            if (lanes[card.lane()]) throw new IllegalArgumentException("Cannot evaluate cards with duplicate lanes");
            lanes[card.lane()] = true;
            ranks.computeIfAbsent(value(card), ignored -> new ArrayList<>()).add(card);
            suits.computeIfAbsent(card.suit(), ignored -> new ArrayList<>()).add(card);
        }
 
        // High card: always available, even if the hand is better.
        List<Integer> values = new ArrayList<>(ranks.keySet());
        values.sort(Comparator.reverseOrder());
        Best best = new Best();
        best.offer(HandType.HIGH_CARD, ranks.get(values.get(0)), descendingValues(creatures, 0, 0));
 
        // Suit checks: if there are 5 suited cards, check for flushes and straights; if there are 4 suited cards, check for Four of a suit.
        for (List<HandCard> suited : suits.values()) {
            if (suited.size() == 5) {
                List<Integer> suitedValues = descendingValues(suited, 0, 0);
                int straight = straightHigh(suitedValues);
                if (straight != 0) {
                    best.offer(straight == 14 ? HandType.ROYAL_FLUSH : HandType.STRAIGHT_FLUSH,
                            suited, List.of(straight));
                }
                best.offer(HandType.FLUSH, suited, suitedValues);
            } else if (suited.size() == 4) {
                // Sort suited cards by value and select the three highest as attackers.
                List<HandCard> sorted = new ArrayList<>(suited);
                sorted.sort(Comparator.comparingInt(HandEvaluator::value).reversed());
                List<HandCard> attackers = new ArrayList<>(sorted.subList(0, 3));
 
                List<Integer> tie = descendingValues(suited, 0, 0);
                Suit suit = suited.get(0).suit();
                int kicker = 0;
                for (HandCard card : creatures) {
                    if (card.suit() != suit) kicker = Math.max(kicker, value(card));
                }
                if (kicker != 0) tie.add(kicker);
                best.offer(HandType.FOUR_OF_A_SUIT, attackers, tie);
            }
        }
 
        // Checks on ranks, if better hand is found replaces the old one
        int four = 0;
        int three = 0;
        List<Integer> pairs = new ArrayList<>(2);
        for (int v : values) {
            switch (ranks.get(v).size()) {
                case 5 -> best.offer(HandType.FIVE_OF_A_KIND, ranks.get(v), List.of(v));
                case 4 -> { if (four == 0) four = v; }
                case 3 -> { if (three == 0) three = v; }
                case 2 -> pairs.add(v);
                default -> { }
            }
        }
 
        if (creatures.size() == 5) {
            int straight = straightHigh(values);
            if (straight != 0) best.offer(HandType.STRAIGHT, creatures, List.of(straight));
        }
        if (four != 0) {
            List<Integer> tie = new ArrayList<>();
            tie.add(four);
            tie.addAll(descendingValues(creatures, four, 0));
            best.offer(HandType.FOUR_OF_A_KIND, ranks.get(four), tie);
        }
        if (three != 0 && !pairs.isEmpty()) {
            List<HandCard> attackers = new ArrayList<>(ranks.get(three));
            attackers.addAll(ranks.get(pairs.get(0)));
            best.offer(HandType.FULL_HOUSE, attackers, List.of(three, pairs.get(0)));
        }
        if (three != 0) {
            List<Integer> tie = new ArrayList<>();
            tie.add(three);
            tie.addAll(descendingValues(creatures, three, 0));
            best.offer(HandType.THREE_OF_A_KIND, ranks.get(three), tie);
        }
        if (pairs.size() >= 2) {
            int high = pairs.get(0);
            int low = pairs.get(1);
            List<HandCard> attackers = new ArrayList<>(ranks.get(high));
            attackers.addAll(ranks.get(low));
            List<Integer> tie = new ArrayList<>(List.of(high, low));
            tie.addAll(descendingValues(creatures, high, low));
            best.offer(HandType.TWO_PAIR, attackers, tie);
        }
        if (!pairs.isEmpty()) {
            int pair = pairs.get(0);
            List<Integer> tie = new ArrayList<>();
            tie.add(pair);
            tie.addAll(descendingValues(creatures, pair, 0));
            best.offer(HandType.PAIR, ranks.get(pair), tie);
        }
 
        return new HandResult(best.type, lanesOf(best.attackers), best.tie);
    }
 
    // Keeps the hand found 'till now. 
    private static final class Best {
        HandType type = HandType.NONE;
        List<HandCard> attackers = List.of();
        List<Integer> tie = List.of();
 
        void offer(HandType candidate, List<HandCard> candidateAttackers, List<Integer> candidateTie) {
            if (candidate.ordinal() < type.ordinal()) {
                type = candidate;
                attackers = candidateAttackers;
                tie = candidateTie;
            }
        }
    }
 
    /** Return card values in descending order, excluding two values (0 means no exclusion). */
    private static List<Integer> descendingValues(List<HandCard> cards, int excludedA, int excludedB) {
        List<Integer> out = new ArrayList<>(cards.size());
        for (HandCard card : cards) {
            int v = value(card);
            if (v != excludedA && v != excludedB) out.add(v);
        }
        out.sort(Comparator.reverseOrder());
        return out;
    }
 
    /** Return the high card of a straight, or 0 if there is no straight. Handles A-2-3-4-5 as 5-high. */
    private static int straightHigh(List<Integer> descending) {
        if (descending.size() != 5) return 0;
        boolean run = true;
        for (int i = 1; i < 5; i++) run &= descending.get(i - 1) - descending.get(i) == 1;
        if (run) return descending.get(0);
        if (descending.get(0) == 14 && descending.get(1) == 5 && descending.get(2) == 4
                && descending.get(3) == 3 && descending.get(4) == 2) return 5;
        return 0;
    }
 
    // Ace is worth 14 in comparisons.
    private static int value(HandCard card) {
        return card.rank() == HandCard.ACE ? HandResult.ACE_HIGH : card.rank();
    }
 
    private static List<Integer> lanesOf(List<HandCard> cards) {
        List<Integer> lanes = new ArrayList<>(cards.size());
        for (HandCard card : cards) lanes.add(card.lane());
        lanes.sort(Integer::compareTo);
        return lanes;
    }
    /**
     * Compares two results: positive if {@code a} is the better hand, negative if {@code b} is, zero for a
     * tie. A lower {@link HandType} ordinal wins; equal types compare their {@link HandResult#tiebreak()}.
     * When one list is shorter (fewer creatures), each missing value counts as 0, so a kicker of 5 beats
     * an empty card. Suits and lanes never break a tie.
     */
    public static int compare(HandResult a, HandResult b) {
        int typeComparison = Integer.compare(b.type().ordinal(), a.type().ordinal());
        if (typeComparison != 0) {
            return typeComparison;
        }

        List<Integer> aTiebreak = a.tiebreak();
        List<Integer> bTiebreak = b.tiebreak();
        int length = Math.max(aTiebreak.size(), bTiebreak.size());
        for (int i = 0; i < length; i++) {
            int aValue = i < aTiebreak.size() ? aTiebreak.get(i) : 0;
            int bValue = i < bTiebreak.size() ? bTiebreak.get(i) : 0;
            int valueComparison = Integer.compare(aValue, bValue);
            if (valueComparison != 0) {
                return valueComparison;
            }
        }
        return 0;
    }

    /**
     * Hand multiplier for spill damage, in per mille (1100 = x1.1), read from
     * {@code balance.hand_multipliers_permille} in the ruleset. {@link HandType#NONE} is 0.
     *
     * @throws quorma.gameengine.data.RulesetException if the ruleset has no entry for {@code type}
     */
    public static int spillMultiplierPermille(Ruleset rules, HandType type) {
        return rules.handMultiplierPermille(type.name());
    }
}
