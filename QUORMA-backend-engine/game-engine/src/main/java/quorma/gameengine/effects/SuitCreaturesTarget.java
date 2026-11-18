package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import quorma.gameengine.data.Value;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** S12: the creatures of the player of one suit ({@code suit} param). */
public final class SuitCreaturesTarget implements Target {

    @Override
    public String id() {
        return TargetIds.SUIT_CREATURES;
    }

    @Override
    public List<Creature> select(TargetView view, Side side, Creature source, TriggerContext ctx, BoundExpansion e) {
        List<Creature> out = new ArrayList<>();
        Suit suit = suit(e);
        view.creatures(side).stream().filter(c -> c.card().suit() == suit).forEach(c -> TargetSupport.addLiving(out, c));
        return out;
    }

    @Override
    public List<Creature> ofPlayerAbility(TargetView view, Side side, Creature source, BoundExpansion e) {
        Suit suit = suit(e);
        List<Creature> out = new ArrayList<>();
        for (Creature c : view.creatures(side)) {
            if (!c.destroyed() && c.card().suit() == suit) {
                out.add(c);
            }
        }
        return out;
    }

    @Override
    public boolean acceptsParams(Map<String, Value> params) {
        return TargetParams.accepts(params, Map.of("suit", v -> v instanceof Value.StrV s && ConditionParams.SUITS.contains(s.v())))
                && params.get("suit") instanceof Value.StrV;
    }

    private static Suit suit(BoundExpansion e) {
        return Suit.valueOf(((Value.StrV) e.targetParams().get("suit")).v().toUpperCase(Locale.ROOT));
    }
}
