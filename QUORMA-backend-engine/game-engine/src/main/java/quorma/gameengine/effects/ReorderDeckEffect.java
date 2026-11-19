package quorma.gameengine.effects;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;

/**
 * E49 on S47: the highest-ranked creature among the top {@code count} cards (a target param) goes on top, or to the
 * bottom, of the deck. Nothing happens if those cards hold no creature.
 */
public final class ReorderDeckEffect implements PlayerEffect {

    public static final String ID = "E49";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.I);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.none().strings(Map.of("position", Set.of("top", "bottom"))));
    }

    @Override
    public Optional<Resolution> apply(PlayerEffectContext ctx, Use use) {
        BoundExpansion e = use.expansion();
        Side owner = use.ownerSide();
        PlayerState p = ctx.player(owner);
        int count = ((Value.IntV) e.targetParams().get("count")).v() > Integer.MAX_VALUE ? Integer.MAX_VALUE
                : Math.toIntExact(((Value.IntV) e.targetParams().get("count")).v());
        int best = -1;
        int bestRank = -1;
        for (int i = 0; i < Math.min(count, p.deck().size()); i++) {
            if (p.deck().get(i) instanceof CreatureCard c) {
                int rank = c.rank() == 1 ? 14 : c.rank();
                if (rank > bestRank) {
                    bestRank = rank;
                    best = i;
                }
            }
        }
        if (best < 0) {
            return Optional.empty();
        }
        Card card = p.deck().remove(best);
        String position = e.stringParam("position").orElse("top");
        if (position.equals("top")) {
            p.deck().add(0, card);
        } else {
            p.deck().add(card);
        }
        use.log().add(new MatchEvent.CardMoved(owner, card.id(), position));
        return Optional.empty();
    }
}
