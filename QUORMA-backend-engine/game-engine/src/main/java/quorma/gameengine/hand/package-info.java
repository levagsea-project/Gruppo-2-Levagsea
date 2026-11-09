/**
 * Poker hand evaluation: which hand a board forms, which creatures attack, and the spill multiplier.
 *
 * <p>Rules for everything in this package: deterministic, integers only (multipliers in per mille), lists in
 * ascending lane order, no dependence on input list order. The evaluator sees only lane, effective rank and
 * effective suit ({@link quorma.gameengine.hand.HandCard}), never ATK, DEF or keywords.
 *
 * <p>Tie-break values use Ace = 14 ({@link quorma.gameengine.hand.HandResult#tiebreak()}).
 */
package quorma.gameengine.hand;
