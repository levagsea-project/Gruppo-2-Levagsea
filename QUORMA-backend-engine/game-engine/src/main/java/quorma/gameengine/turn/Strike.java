package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.List;
import quorma.gameengine.data.RulesetException;
import quorma.gameengine.data.Value;
import quorma.gameengine.effects.AtkToBaseEffect;
import quorma.gameengine.effects.DamageReductionEffect;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.effects.IgnoreMitigationEffect;
import quorma.gameengine.effects.LethalDamageEffect;
import quorma.gameengine.effects.PreventCombatDamageEffect;
import quorma.gameengine.effects.PreventDamageEffect;
import quorma.gameengine.effects.SpillDefShareEffect;
import quorma.gameengine.effects.SpillMultiplierEffect;
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
 * @param decisive        the suit synergy tiers without which this blow would end differently (telemetry), see
 *                        {@link #decisiveTiers}; empty for a blow only worked out (not about to land)
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
        boolean lethal,
        int handMultiplierPermille,
        int spillMultiplierPermille,
        int spill,
        List<Modifier> damageModifiers,
        List<Modifier> spillModifiers,
        List<Decisive> decisive) {

    Strike {
        damageModifiers = List.copyOf(damageModifiers);
        spillModifiers = List.copyOf(spillModifiers);
        decisive = List.copyOf(decisive);
    }

    /** A suit synergy tier of a side (a tier is the ability id of its expansions). */
    record Tier(Side side, String id) {}

    /**
     * A tier that decided a blow: {@code kill} when the target would not have been destroyed without it (or would
     * have been destroyed with it), {@code spill} when the spill would have had another value.
     */
    record Decisive(Tier tier, boolean kill, boolean spill) {}

    /**
     * The blow of {@code attacker} on {@code target}. With {@code land} true the blow is about to land: a T21
     * replacement of the target that prevents damage (E29) is used up; false only works the numbers out (C21).
     */
    static Strike of(Game g, Board b, CycleStart start, Side attackerSide, Creature attacker, Side targetSide,
            Creature target, boolean land) {
        Strike strike = of(g, b, start, attackerSide, attacker, targetSide, target, land, null);
        return land ? strike.withDecisive(decisiveTiers(g, b, start, attackerSide, attacker, targetSide, target, strike)) : strike;
    }

    /**
     * The tiers of the blow's own modifiers without which the blow would end differently: the blow is worked out
     * again, only for that tier, with its ATK bonuses and its spill replacements left out, and the two are compared
     * (both without the use of an E29 prevention, which would be spent twice). Only the single blow is recomputed,
     * never the rest of the match (decision {@code 2026-11-24-synergy-telemetry-engine}).
     */
    private static List<Decisive> decisiveTiers(Game g, Board b, CycleStart start, Side attackerSide, Creature attacker,
            Side targetSide, Creature target, Strike landed) {
        List<Tier> tiers = new ArrayList<>();
        for (List<Modifier> modifiers : List.of(landed.damageModifiers(), landed.spillModifiers())) {
            for (Modifier m : modifiers) {
                Tier tier = new Tier(m.source().side(), m.abilityId());
                if (g.rules.synergies().containsKey(m.abilityId()) && !tiers.contains(tier)) {
                    tiers.add(tier);
                }
            }
        }
        List<Decisive> out = new ArrayList<>();
        if (tiers.isEmpty()) {
            return out;
        }
        Strike with = of(g, b, start, attackerSide, attacker, targetSide, target, false, null);
        for (Tier tier : tiers) {
            Strike without = of(g, b, start, attackerSide, attacker, targetSide, target, false, tier);
            boolean kill = with.killed() != without.killed();
            boolean spill = with.spill() != without.spill();
            if (kill || spill) {
                out.add(new Decisive(tier, kill, spill));
            }
        }
        return out;
    }

    /** The blow worked out with the modifiers of {@code without} (a tier) left out; null leaves nothing out. */
    private static Strike of(Game g, Board b, CycleStart start, Side attackerSide, Creature attacker, Side targetSide,
            Creature target, boolean land, Tier without) {
        int handMultiplierPermille = g.evaluation.multiplier(attackerSide);
        CreatureRef attackerRef = CreatureRef.of(attackerSide, attacker);

        // ATK that static effects (suit synergies, abilities, spells) add, worked out on the board as it is now.
        List<Modifier> atkBonuses = new ArrayList<>(b.atkBonuses(attacker, target, start));
        if (without != null) {
            atkBonuses.removeIf(m -> without.id().equals(m.abilityId()) && without.side() == m.source().side());
        }
        long atk = b.ownAtk(attacker);
        for (Modifier bonus : atkBonuses) {
            atk += bonus.kind() == Modifier.Kind.ATK_MALUS ? -bonus.value() : bonus.value();
        }
        atk = Math.max(0, atk);
        List<Modifier> damageModifiers = new ArrayList<>(atkBonuses);
        if (attacker.atkSetToBase()) {
            atk = attacker.card().atk();
            damageModifiers.add(new Modifier(attackerRef, attacker.atkSetToBaseBy(), AtkToBaseEffect.ID, Modifier.Kind.ATK_SET,
                    attacker.card().atk()));
        }
        int advantage = g.rules.advantagePermille(
                attacker.card().subfaction().dataName(), target.card().subfaction().dataName());
        int effectiveAtk = Damage.effectiveAtk(Math.toIntExact(atk), advantage);

        // The target's reductions, in card order.
        List<Modifier> reductions = new ArrayList<>();
        List<Integer> pcts = new ArrayList<>();
        int flat = 0;
        int minDamage = 0;
        for (Board.Active a : b.reductions(target)) {
            BoundExpansion e = a.e();
            Creature from = a.sourceOr(target);
            CreatureRef fromRef = CreatureRef.of(b.sideOf(from), from);
            DamageReductionEffect.Reduction reduction = DamageReductionEffect.reduction(e);
            if (reduction.flat()) {
                flat = Math.addExact(flat, reduction.value());
                minDamage = Math.max(minDamage, reduction.minDamage());
                reductions.add(new Modifier(fromRef, e.abilityId(), e.effectId(),
                        Modifier.Kind.DAMAGE_REDUCTION_FLAT, reduction.value()));
            } else {
                pcts.add(reduction.value());
                reductions.add(new Modifier(fromRef, e.abilityId(), e.effectId(),
                        Modifier.Kind.DAMAGE_REDUCTION_PCT, reduction.value()));
            }
        }

        // The attacker's pierce: several add up, at most 100.
        List<Modifier> piercing = new ArrayList<>();
        int pierce = 0;
        for (Board.Active a : b.reaching(IgnoreMitigationEffect.ID, attacker, Board.Ctx.NONE)) {
            pierce = Math.min(100, pierce + a.e().intParam("pct"));
            piercing.add(new Modifier(attackerRef, a.e().abilityId(), a.e().effectId(), Modifier.Kind.PIERCE_PCT,
                    a.e().intParam("pct")));
        }

        int mitigation = Damage.mitigationPermille(pcts, pierce, mitigationFloor(g));
        int damage = Damage.damage(effectiveAtk, Damage.pierce(flat, pierce), mitigation, minDamage);

        if (effectiveAtk > 0) {
            damageModifiers.addAll(reductions);
            if (!reductions.isEmpty()) {
                damageModifiers.addAll(piercing); // Perforante changes nothing against a target with no reductions
            }
        }

        // E23: an effect stops the attacker from damaging this target; E29: the target prevents the next damage.
        String preventedBy = damage > 0 ? attacker.cannotDamageBy(target) : null;
        if (preventedBy != null) {
            damageModifiers.add(new Modifier(CreatureRef.of(targetSide, target), preventedBy, PreventCombatDamageEffect.ID,
                    Modifier.Kind.DAMAGE_PREVENTED, damage));
            damage = 0;
        }
        if (damage > 0 && land) {
            Destruction.Prevention p = g.destruction.preventDamage(target, damage);
            if (p.left() != damage) {
                damageModifiers.add(new Modifier(CreatureRef.of(targetSide, target), p.abilityId(), PreventDamageEffect.ID,
                        Modifier.Kind.DAMAGE_PREVENTED, damage - p.left()));
                damage = p.left();
            }
        }

        int defBefore = target.currentDef();
        int defAfter = Math.subtractExact(defBefore, damage);
        List<Board.Active> lethalOnes = damage > 0 && defAfter > 0
                ? b.reaching(LethalDamageEffect.ID, attacker, Board.Ctx.NONE) : List.of();
        boolean lethal = !lethalOnes.isEmpty();
        if (lethal) {
            damageModifiers.add(new Modifier(attackerRef, lethalOnes.get(0).e().abilityId(), LethalDamageEffect.ID,
                    Modifier.Kind.LETHAL, 0));
        }
        boolean killed = defAfter <= 0 || lethal;

        // The attacker's replacement effects when it is about to deal spill (T34), in ability order, then the spells'
        // that reach it, only on a kill: E21 changes the hand multiplier, E22 the DEF the spill uses.
        List<Modifier> spillModifiers = new ArrayList<>();
        List<Integer> factors = new ArrayList<>();
        List<Integer> defShares = new ArrayList<>();
        if (killed) {
            for (Board.Active a : b.replacements("T34", attacker)) {
                BoundExpansion e = a.e();
                Creature from = a.sourceOr(attacker);
                CreatureRef fromRef = CreatureRef.of(b.sideOf(from), from);
                if (without != null && without.id().equals(e.abilityId()) && without.side() == fromRef.side()) {
                    continue;
                }
                if (e.effectId().equals(SpillMultiplierEffect.ID)) {
                    factors.add(e.intParam("factor"));
                    spillModifiers.add(new Modifier(fromRef, e.abilityId(), e.effectId(),
                            Modifier.Kind.SPILL_MULTIPLIER, e.intParam("factor")));
                } else if (e.effectId().equals(SpillDefShareEffect.ID)) {
                    defShares.add(e.intParam("permille"));
                    spillModifiers.add(new Modifier(fromRef, e.abilityId(), e.effectId(),
                            Modifier.Kind.SPILL_DEF_PERMILLE, e.intParam("permille")));
                }
            }
        }
        int spillMultiplier = Damage.spillMultiplier(handMultiplierPermille, factors);
        int spill = killed ? Damage.spill(damage, Damage.spillDef(defBefore, defShares), spillMultiplier) : 0;

        return new Strike(Math.toIntExact(atk), advantage, effectiveAtk, mitigation, damage, defBefore, defAfter, killed,
                lethal, handMultiplierPermille, spillMultiplier, spill, damageModifiers, spillModifiers, List.of());
    }

    /**
     * The blow when the target would have been destroyed but was saved (E12: full DEF; E11: {@code set_def}): nothing
     * is destroyed, and the blow deals no spill unless {@code keepSpill}.
     */
    Strike saved(int defAfterSave, boolean keepSpill) {
        return new Strike(atk, advantagePermille, effectiveAtk, mitigationPermille, damage, defBefore, defAfterSave,
                false, false, handMultiplierPermille, keepSpill ? spillMultiplierPermille : handMultiplierPermille,
                keepSpill ? spill : 0, damageModifiers, keepSpill ? spillModifiers : List.of(), decisive);
    }

    /** The blow of a Cycle 1 mutual kill (ruleset 9.7): no spill, so Traboccante does not apply either. */
    Strike withoutSpill() {
        return new Strike(atk, advantagePermille, effectiveAtk, mitigationPermille, damage, defBefore, defAfter,
                killed, lethal, handMultiplierPermille, handMultiplierPermille, 0, damageModifiers, List.of(), decisive);
    }

    private Strike withDecisive(List<Decisive> decisive) {
        return new Strike(atk, advantagePermille, effectiveAtk, mitigationPermille, damage, defBefore, defAfter, killed,
                lethal, handMultiplierPermille, spillMultiplierPermille, spill, damageModifiers, spillModifiers, decisive);
    }

    AttackResolved event(Attack attack, int cycle, boolean mutualKill) {
        List<Modifier> modifiers = new ArrayList<>(damageModifiers);
        modifiers.addAll(spillModifiers);
        return new AttackResolved(attack, cycle, atk, advantagePermille, effectiveAtk, mitigationPermille, damage,
                defBefore, defAfter, killed, mutualKill, handMultiplierPermille, spillMultiplierPermille, spill,
                modifiers);
    }

    private static int mitigationFloor(Game g) {
        if (g.rules.global("mitigation_floor_permille").orElse(null) instanceof Value.IntV n) {
            return Math.toIntExact(n.v());
        }
        throw new RulesetException("balance.json#globals.mitigation_floor_permille: missing or not an integer");
    }
}
