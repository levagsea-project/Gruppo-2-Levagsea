package quorma.gameengine.effects;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.log.TargetRef;
import quorma.gameengine.model.LaneEffect;
import quorma.gameengine.model.PlayerState;

/**
 * E64: registers an effect ({@code effect_id}, with {@code effect_params} and {@code duration}) on the lane of the
 * creature whose ability it is, for the next creature placed there (Posto tenuto). Triggered effects only.
 */
public final class PendingLaneEffect implements PlayerEffect {

    public static final String ID = "E64";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.T);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.none().strings(Map.of("effect_id", Set.of("E1", "E2", "E6"), "duration", Set.of()))
                .prefixes(Set.of("effect_params")));
    }

    @Override
    public Optional<Resolution> apply(PlayerEffectContext ctx, Use use) {
        BoundExpansion e = use.expansion();
        PlayerState p = ctx.player(use.ownerSide());
        int lane = use.source().lane();
        p.addLaneEffect(new LaneEffect(lane, use.source().card().id(), pending(e)));
        return Optional.of(new Resolution(new TargetRef.LaneRef(use.ownerSide(), lane), List.of()));
    }

    /** The effect to run on the next creature placed in the lane: {@code e64}'s inner effect, aimed at S27. */
    private static BoundExpansion pending(BoundExpansion e64) {
        String effectId = e64.stringParam("effect_id").orElseThrow();
        String duration = e64.stringParam("duration").orElseThrow();
        return new BoundExpansion(e64.abilityId(), null, Map.of(), effectId, TargetIds.REPLACEMENT, Map.of(), List.of(),
                duration, ResClass.I, e64.nested("effect_params"), OptionalInt.empty(), Optional.empty(), false, 0,
                false, e64.index());
    }
}
