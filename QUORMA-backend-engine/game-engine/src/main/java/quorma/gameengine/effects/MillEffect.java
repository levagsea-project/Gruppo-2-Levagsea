package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.NON_NEGATIVE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.PlayerState;

/** E57: the top {@code count} cards of the player's deck go to the discard pile with no effect. */
public final class MillEffect implements PlayerEffect {

    @Override
    public String id() {
        return "E57";
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.I);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.ints(Map.of("count", NON_NEGATIVE)));
    }

    @Override
    public Optional<Resolution> apply(PlayerEffectContext ctx, Use use) {
        BoundExpansion e = use.expansion();
        PlayerState p = ctx.player(use.playerSide());
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < e.intParam("count") && !p.deck().isEmpty(); i++) {
            Card c = p.deck().remove(0);
            p.discard().add(c);
            ids.add(c.id());
        }
        use.log().add(new MatchEvent.CardsMilled(use.playerSide(), e.abilityId(), ids));
        return Optional.empty();
    }
}
