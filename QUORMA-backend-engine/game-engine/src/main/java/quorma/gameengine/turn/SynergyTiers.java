package quorma.gameengine.turn;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.data.SynergyDef;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Side;

/**
 * Which suit synergy tiers are active (ruleset 7.10) and who has them. The tiers are data ({@code synergies.json});
 * what their expansions do is done by the code that runs any ability with that trigger, effect and target, for
 * example {@code StaticEffects} for the static ones.
 */
final class SynergyTiers {

    private SynergyTiers() {}

    /** Counts the suits of each side and lists the active tiers, replacements applied (7.10.1, 7.10.2). */
    static Synergies evaluate(Ruleset rules, MatchState state) {
        Map<Side, List<String>> active = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            Map<String, Integer> counts = new HashMap<>();
            for (Creature c : state.player(side).creatures()) {
                counts.merge(c.card().suit().name(), 1, Integer::sum);
            }
            List<SynergyDef> reached = rules.synergies().values().stream()
                    .filter(d -> counts.getOrDefault(d.suit(), 0) >= d.tier())
                    .toList();
            Set<String> replaced = new HashSet<>();
            reached.forEach(d -> replaced.addAll(d.replaces()));
            active.put(side, reached.stream().map(SynergyDef::id).filter(id -> !replaced.contains(id)).toList());
        }
        return new Synergies(active);
    }

    /**
     * Gives each creature a copy of the expansions of the active tiers of its suit (instantiation
     * {@code per_creature}: the creature is the source), and clears what it had from an earlier turn.
     */
    static void bind(AbilityBinder binder, Ruleset rules, MatchState state, Synergies synergies) {
        Map<String, List<BoundExpansion>> bound = new LinkedHashMap<>();
        for (Side side : Side.values()) {
            for (Creature creature : state.player(side).creatures()) {
                List<BoundExpansion> own = new java.util.ArrayList<>();
                for (String id : synergies.tiers(side)) {
                    SynergyDef tier = rules.synergies().get(id);
                    if (tier.perCreature() && tier.suit().equals(creature.card().suit().name())) {
                        own.addAll(bound.computeIfAbsent(id, k -> binder.bindSynergy(tier)));
                    }
                }
                creature.setSynergyAbilities(own);
            }
        }
    }
}
