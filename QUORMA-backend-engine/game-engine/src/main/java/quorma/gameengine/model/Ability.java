package quorma.gameengine.model;

/**
 * Something a card can do. Every kind of card (creature, spell, equipment) lists its abilities the same way,
 * and any card can have either kind of ability:
 *
 * <ul>
 *   <li>{@link AbilityRef}: a reference to a named keyword or evolution, with the card's own parameters;
 *   <li>{@link InlineEffect}: a whole effect written on the card, as expansions.
 * </ul>
 *
 * Both end up as the same trigger, effect and target expansions of the data files. What differs between
 * card kinds is when they fire (a creature's by trigger, a spell's or equipment's when drawn) and which
 * targets make sense, not how they are written.
 */
public sealed interface Ability permits AbilityRef, InlineEffect {}
