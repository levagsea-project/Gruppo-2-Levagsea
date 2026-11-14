package quorma.gameengine.log;

import java.util.List;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.MatchResult;
import quorma.gameengine.model.Side;

/**
 * Everything that happens in a turn, in the order it happens. This is the in-memory base of the match log: a
 * later task turns it into JSON. Events carry only numbers, ids and lanes, never object references to state.
 */
public sealed interface MatchEvent {

    /** Evaluate: a player's hand, fixed for the turn (ruleset 7.9). */
    record HandEvaluated(int turn, Side side, HandResult hand, int multiplierPermille) implements MatchEvent {}

    /** Declare: an attack and its frozen target (ruleset 8). */
    record TargetDeclared(Attack attack) implements MatchEvent {}

    /** An attack that did not happen. {@code cycle} is 1 or 2. */
    record AttackSkipped(Attack attack, int cycle, SkipReason reason) implements MatchEvent {}

    /**
     * An attack that happened.
     *
     * @param atk                     the attacker's current ATK at the blow
     * @param mitigationPermille      share of the damage that goes through the target's percentage reductions
     * @param damage                  damage dealt to the target
     * @param defBefore               the target DEF just before this blow, the value spill uses
     * @param handMultiplierPermille  the attacker's hand multiplier
     * @param spillMultiplierPermille the multiplier spill used
     * @param spill                   damage to the opponent's life, 0 unless the attack killed
     * @param modifiers               the abilities that changed the numbers
     */
    record AttackResolved(
            Attack attack,
            int cycle,
            int atk,
            int advantagePermille,
            int effectiveAtk,
            int mitigationPermille,
            int damage,
            int defBefore,
            int defAfter,
            boolean killed,
            boolean mutualKill,
            int handMultiplierPermille,
            int spillMultiplierPermille,
            int spill,
            List<Modifier> modifiers)
            implements MatchEvent {

        public AttackResolved {
            modifiers = List.copyOf(modifiers);
        }
    }

    /** A creature was flagged destroyed. It stays in its lane until Refill. */
    record CreatureDestroyed(Side side, int lane, String cardId) implements MatchEvent {}

    /** Refill: a destroyed creature went to the discard pile and its lane is empty. */
    record CreatureDiscarded(Side side, int lane, String cardId) implements MatchEvent {}

    /** Refill: a creature was drawn into an empty lane. */
    record CardDrawn(Side side, int lane, String cardId) implements MatchEvent {}

    /** Refill: a drawn spell took effect and was discarded (ruleset 10.5). */
    record SpellResolved(Side side, int lane, String cardId) implements MatchEvent {}

    /** Refill: a drawn equipment waits for the next creature drawn (ruleset 10.6). */
    record EquipmentPending(Side side, String cardId) implements MatchEvent {}

    /** Refill: a waiting equipment was applied to the creature just drawn. */
    record EquipmentApplied(Side side, int lane, String equipmentId, String creatureId) implements MatchEvent {}

    /** Refill: an equipment found no creature to receive it (ruleset 10.8). */
    record EquipmentLost(Side side, String cardId) implements MatchEvent {}

    /** Refill: the deck was empty and the lane stays empty (ruleset 10.8). */
    record LaneLeftEmpty(Side side, int lane) implements MatchEvent {}

    /** End of turn: the match is over (ruleset 11). */
    record MatchEnded(int turn, MatchResult result) implements MatchEvent {}
}
