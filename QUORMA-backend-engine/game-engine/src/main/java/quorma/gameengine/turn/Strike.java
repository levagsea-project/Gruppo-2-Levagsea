package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.List;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.data.RulesetException;
import quorma.gameengine.data.Value;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.log.Modifier;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/**
 * The numbers of one blow, worked out from the creatures as they are when it lands, with the abilities that
 * changed them. Arithmetic in {@link Damage}.
 *
 * @param damageModifiers effects that changed the damage, in card order; empty when the effective ATK is 0
 * @param spillModifiers  effects that changed the spill, in card order; empty unless the blow kills
 * @param spill           as if the blow were not part of a mutual kill; see {@link #withoutSpill()}
 */
record Strike(
        int atk,
        int advantagePermille,
        int effectiveAtk,
        int mitigationPermille,
        int damage,
        int defBefore,
        int defAfter,
        boolean killed,
        int handMultiplierPermille,
        int spillMultiplierPermille,
        int spill,
        List<Modifier> damageModifiers,
        List<Modifier> spillModifiers) {

    Strike {
        damageModifiers = List.copyOf(damageModifiers);
        spillModifiers = List.copyOf(spillModifiers);
    }

    static Strike of(Ruleset rules, Side attackerSide, Creature attacker, Side targetSide, Creature target,
            int handMultiplierPermille) {
        CreatureRef attackerRef = CreatureRef.of(attackerSide, attacker);
        CreatureRef targetRef = CreatureRef.of(targetSide, target);

        int atk = attacker.currentAtk();
        int advantage = rules.advantagePermille(
                attacker.card().subfaction().dataName(), target.card().subfaction().dataName());
        int effectiveAtk = Damage.effectiveAtk(atk, advantage);

        // The target's reductions, in card order.
        List<Modifier> reductions = new ArrayList<>();
        List<Integer> pcts = new ArrayList<>();
        int flat = 0;
        int minDamage = 0;
        for (BoundExpansion e : effects(target, "T0", "E28")) {
            if (e.stringParam("mode").orElseThrow().equals("flat")) {
                flat = Math.addExact(flat, e.intParam("amount"));
                minDamage = Math.max(minDamage, e.intParam("min_damage"));
                reductions.add(new Modifier(targetRef, e.abilityId(), e.effectId(),
                        Modifier.Kind.DAMAGE_REDUCTION_FLAT, e.intParam("amount")));
            } else {
                pcts.add(e.intParam("pct"));
                reductions.add(new Modifier(targetRef, e.abilityId(), e.effectId(),
                        Modifier.Kind.DAMAGE_REDUCTION_PCT, e.intParam("pct")));
            }
        }

        // The attacker's pierce: several add up, at most 100.
        List<Modifier> piercing = new ArrayList<>();
        int pierce = 0;
        for (BoundExpansion e : effects(attacker, "T0", "E56")) {
            pierce = Math.min(100, pierce + e.intParam("pct"));
            piercing.add(new Modifier(attackerRef, e.abilityId(), e.effectId(), Modifier.Kind.PIERCE_PCT, e.intParam("pct")));
        }

        int mitigation = Damage.mitigationPermille(pcts, pierce, mitigationFloor(rules));
        int damage = Damage.damage(effectiveAtk, Damage.pierce(flat, pierce), mitigation, minDamage);

        List<Modifier> damageModifiers = new ArrayList<>();
        if (effectiveAtk > 0) {
            damageModifiers.addAll(reductions);
            if (!reductions.isEmpty()) {
                damageModifiers.addAll(piercing); // Perforante changes nothing against a target with no reductions
            }
        }

        int defBefore = target.currentDef();
        int defAfter = Math.subtractExact(defBefore, damage);
        boolean killed = defAfter <= 0;

        // The attacker's Traboccante (E21, T34 "about to deal spill"): only on a kill.
        List<Modifier> spillModifiers = new ArrayList<>();
        List<Integer> factors = new ArrayList<>();
        if (killed) {
            for (BoundExpansion e : effects(attacker, "T34", "E21")) {
                factors.add(e.intParam("factor"));
                spillModifiers.add(new Modifier(attackerRef, e.abilityId(), e.effectId(),
                        Modifier.Kind.SPILL_MULTIPLIER, e.intParam("factor")));
            }
        }
        int spillMultiplier = Damage.spillMultiplier(handMultiplierPermille, factors);
        int spill = killed ? Damage.spill(damage, defBefore, spillMultiplier) : 0;

        return new Strike(atk, advantage, effectiveAtk, mitigation, damage, defBefore, defAfter, killed,
                handMultiplierPermille, spillMultiplier, spill, damageModifiers, spillModifiers);
    }

    /** The blow of a Cycle 1 mutual kill (ruleset 9.7): no spill, so Traboccante does not apply either. */
    Strike withoutSpill() {
        return new Strike(atk, advantagePermille, effectiveAtk, mitigationPermille, damage, defBefore, defAfter,
                killed, handMultiplierPermille, handMultiplierPermille, 0, damageModifiers, List.of());
    }

    AttackResolved event(Attack attack, int cycle, boolean mutualKill) {
        List<Modifier> modifiers = new ArrayList<>(damageModifiers);
        modifiers.addAll(spillModifiers);
        return new AttackResolved(attack, cycle, atk, advantagePermille, effectiveAtk, mitigationPermille, damage,
                defBefore, defAfter, killed, mutualKill, handMultiplierPermille, spillMultiplierPermille, spill,
                modifiers);
    }

    private static List<BoundExpansion> effects(Creature creature, String triggerId, String effectId) {
        return creature.abilities(triggerId).stream().filter(e -> e.effectId().equals(effectId)).toList();
    }

    private static int mitigationFloor(Ruleset rules) {
        if (rules.global("mitigation_floor_permille").orElse(null) instanceof Value.IntV n) {
            return Math.toIntExact(n.v());
        }
        throw new RulesetException("balance.json#globals.mitigation_floor_permille: missing or not an integer");
    }
}
