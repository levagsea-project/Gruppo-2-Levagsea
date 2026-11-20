package quorma.gameengine.effects;

import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import quorma.gameengine.data.Value;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** S44: the creatures next to the one that took damage (or next to a chosen one). */
public final class DamagedNeighboursTarget implements Target {

    @Override
    public String id() {
        return "S44";
    }

    @Override
    public List<Creature> select(TargetView view, Side side, Creature source, TriggerContext ctx, BoundExpansion e) {
        List<Creature> out = new ArrayList<>();
        if (ctx.damaged() != null) {
            view.neighbours(ctx.damagedSide(), ctx.damaged()).forEach(out::add);
        }
        return out;
    }

    @Override
    public List<Creature> selectImmediate(TargetView view, BoundExpansion e, Side owner, Creature receiver,
            Map<String, Creature> chosen) {
        List<Creature> out = new ArrayList<>();
        String of = e.targetParams().get("of") instanceof Value.StrV s ? s.v() : null;
        Creature anchor = of == null ? null : chosen.get(of);
        if (anchor != null) {
            Side anchorSide = view.creatures(owner).contains(anchor) ? owner : owner.other();
            for (Creature n : view.neighbours(anchorSide, anchor)) {
                if (view.filtersHold(e.filters(), owner, anchorSide, n)) {
                    out.add(n);
                }
            }
        }
        return out;
    }

    @Override
    public boolean acceptsParams(Map<String, Value> params) {
        return TargetParams.accepts(params, Map.of("of", v -> v instanceof Value.StrV s
                && Set.of("S33", "S35", "S37", "S38", "S39", "S40").contains(s.v())));
    }
}
