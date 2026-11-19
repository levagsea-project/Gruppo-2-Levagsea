package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.NON_NEGATIVE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.log.TargetRef;
import quorma.gameengine.model.PlayerState;

/** E9: the player gains {@code amount} life. */
public final class GainLifeEffect implements PlayerEffect {

    @Override
    public String id() {
        return "E9";
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.T, ResClass.I);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.ints(Map.of("amount", NON_NEGATIVE)));
    }

    @Override
    public Optional<Resolution> apply(PlayerEffectContext ctx, Use use) {
        PlayerState p = ctx.player(use.playerSide());
        int before = p.life();
        p.gainLife(use.expansion().intParam("amount"));
        return Optional.of(new Resolution(new TargetRef.PlayerRef(use.playerSide()),
                List.of(new StatChange(StatChange.Stat.LIFE, before, p.life()))));
    }
}
