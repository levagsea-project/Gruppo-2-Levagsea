package quorma.gameengine.effects;

import java.util.Map;
import java.util.Set;
import quorma.gameengine.data.ResClass;

/**
 * E34 (The creature counts as adjacent to the creatures of {@code group} (a suit, or its whole player) for effects that need adjacency.) Read by {@code Board.linked}; it has no resolution of its own.
 */
public final class AdjacencyLinkEffect implements EffectDefinition {

    public static final String ID = "E34";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.C);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.none().strings(Map.of("group",
                Set.of("Vincolo", "Frattura", "Spira", "Vestigio", "stesso_giocatore"))));
    }
}
