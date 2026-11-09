/**
 * The state of a match: cards, creatures in lanes, players, the turn, attacks and results.
 *
 * <p>These are plain data classes with no game rules beyond their own invariants.
 * The rules are in {@code quorma.gameengine.turn}.</p>
 * <p>State is mutable on purpose; every collection that affects a result is a list or an array,
 * never a hash set or map.</p>
 */
package quorma.gameengine.model;
