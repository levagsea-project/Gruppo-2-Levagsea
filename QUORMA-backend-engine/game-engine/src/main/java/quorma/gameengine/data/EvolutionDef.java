package quorma.gameengine.data;

import java.util.List;

/**
 * An Evolution ({@code evolutions.json}): a wrapper whose expansions use the trigger {@code T_EVO}, with the real
 * trigger in {@code trigger_params.trigger_id}. It fires at most once per card instance (ruleset 7.10, vocab
 * {@code evolution_rules}).
 */
public record EvolutionDef(String id, String name, String balanceRef, String description, List<ExpansionDef> expandsTo) {

    public EvolutionDef {
        expandsTo = List.copyOf(expandsTo);
    }
}
