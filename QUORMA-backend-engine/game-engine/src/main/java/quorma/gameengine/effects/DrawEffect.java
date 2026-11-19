package quorma.gameengine.effects;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;

/**
 * E35: draws the top card. A creature goes to the lowest empty lane (if there is none it stays on the deck), a spell
 * resolves at once, an equipment waits for the next creature drawn.
 */
public final class DrawEffect implements PlayerEffect {

    @Override
    public String id() {
        return "E35";
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.I);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.none());
    }

    @Override
    public Optional<Resolution> apply(PlayerEffectContext ctx, Use use) {
        Side side = use.playerSide();
        PlayerState p = ctx.player(side);
        if (p.deck().isEmpty()) {
            return Optional.empty();
        }
        if (p.deck().get(0) instanceof CreatureCard) {
            OptionalInt lane = p.lowestEmptyLane();
            if (lane.isPresent()) {
                ctx.draw(side, lane.getAsInt(), use.log());
            }
            return Optional.empty();
        }
        ctx.draw(side, p.lowestEmptyLane().orElse(1), use.log());
        return Optional.empty();
    }
}
