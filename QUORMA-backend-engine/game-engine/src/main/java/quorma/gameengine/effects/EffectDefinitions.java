package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Every effect of the vocabulary the engine implements, by ID: the {@link Effect} and {@link PlayerEffect} classes
 * and the definitions only. Ordered by the number of the ID; never iterated for a result.
 */
public final class EffectDefinitions {

    private static final Map<String, EffectDefinition> BY_ID = build();

    private EffectDefinitions() {}

    /** The definition of {@code effectId}, empty for an ID the engine does not implement. */
    public static Optional<EffectDefinition> find(String effectId) {
        return Optional.ofNullable(BY_ID.get(effectId));
    }

    /** Every definition, in the order of the ID numbers. */
    public static Collection<EffectDefinition> all() {
        return BY_ID.values();
    }

    private static Map<String, EffectDefinition> build() {
        List<EffectDefinition> all = new ArrayList<>();
        all.addAll(Effects.all());
        all.addAll(PlayerEffects.all());
        all.addAll(List.of(
            new SetDefRescueEffect(),
            new FullDefRescueEffect(),
            new ForceEvolutionsEffect(),
            new EvolutionFactorEffect(),
            new SpillMultiplierEffect(),
            new SpillDefShareEffect(),
            new PreventCombatDamageEffect(),
            new RedirectAttackEffect(),
            new PreventAttackEffect(),
            new LethalDamageEffect(),
            new PreventDamageEffect(),
            new ExtraRankEffect(),
            new ReplaceRankEffect(),
            new AdjacencyLinkEffect(),
            new PreventRefillEffect(),
            new HealFactorEffect(),
            new NoAtkReductionEffect(),
            new RankStepEffect(),
            new WildSuitEffect(),
            new IgnoreMitigationEffect(),
            new IsolatedEffect(),
            new NoBonusEffect(),
            new ExcludeFromHandEffect()));
        all.sort(Comparator.comparingInt(d -> Integer.parseInt(d.id().substring(1))));
        Map<String, EffectDefinition> map = new LinkedHashMap<>();
        for (EffectDefinition definition : all) {
            if (map.put(definition.id(), definition) != null) {
                throw new IllegalStateException("two classes for effect " + definition.id());
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
