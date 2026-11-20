package quorma.gameengine.effects;

import java.util.Map;
import quorma.gameengine.data.Value;

/** S47: the top cards of the owner's deck ({@code count} param). */
public final class DeckTopTarget implements Target {

    @Override
    public String id() {
        return TargetIds.DECK_TOP;
    }

    @Override
    public boolean acceptsParams(Map<String, Value> params) {
        return TargetParams.accepts(params, Map.of("count", v -> v instanceof Value.IntV n && n.v() >= 1))
                && params.get("count") instanceof Value.IntV;
    }
}
