package quorma.gameengine.effects;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.log.TargetRef;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;

/**
 * E35: draws the top card. A creature goes to the lowest empty lane (if there is none it stays on the deck), a spell
 * resolves at once, an equipment waits for the next creature drawn.
 */
public final class DrawEffect implements PlayerEffect {

    public static final String ID = "E35";

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
        return Map.of("", ParamRule.none());
    }

    @Override
    public Optional<Resolution> apply(PlayerEffectContext ctx, Use use) {
        Side side = use.playerSide();
        PlayerState p = ctx.player(side);
        Optional<Resolution> resolved = Optional.of(new Resolution(new TargetRef.PlayerRef(side), List.of()));
        if (p.deck().isEmpty()) {
            return resolved;
        }
        if (p.deck().get(0) instanceof CreatureCard) {
            OptionalInt lane = p.lowestEmptyLane();
            if (lane.isPresent()) {
                ctx.draw(side, lane.getAsInt(), use.log());
            }
            return resolved;
        }
        ctx.draw(side, p.lowestEmptyLane().orElse(1), use.log());
        return resolved;
    }
}
