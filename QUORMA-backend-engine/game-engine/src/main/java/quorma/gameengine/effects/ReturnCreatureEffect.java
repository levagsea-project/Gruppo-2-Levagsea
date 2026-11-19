package quorma.gameengine.effects;

import static quorma.gameengine.effects.ParamRule.range;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;

/**
 * E36: places a creature in the lowest empty lane of the owner (or in {@code lane}, if it is empty). With
 * {@code source: discard} it is the last creature that went to the discard pile, back with full DEF and no modifiers;
 * with {@code source: deck}, the top card if it is a creature. Nothing happens with no empty lane or no creature.
 */
public final class ReturnCreatureEffect implements PlayerEffect {

    @Override
    public String id() {
        return "E36";
    }

    @Override
    public Set<ResClass> classes() {
        return Set.of(ResClass.I);
    }

    @Override
    public Map<String, ParamRule> modes() {
        return Map.of("", ParamRule.none().optInts(Map.of("lane", range(1, 5)))
                .optStrings(Map.of("source", Set.of("deck", "discard"))));
    }

    @Override
    public Optional<Resolution> apply(PlayerEffectContext ctx, Use use) {
        BoundExpansion e = use.expansion();
        Side owner = use.ownerSide();
        PlayerState p = ctx.player(owner);
        OptionalInt lane = e.effectParams().containsKey("lane") ? (p.creatureAt(e.intParam("lane")).isEmpty()
                ? OptionalInt.of(e.intParam("lane")) : OptionalInt.empty()) : p.lowestEmptyLane();
        if (lane.isEmpty()) {
            return Optional.empty();
        }
        if (e.stringParam("source").orElse("deck").equals("discard")) {
            for (int i = p.discard().size() - 1; i >= 0; i--) {
                if (p.discard().get(i) instanceof CreatureCard creatureCard) {
                    p.discard().remove(i);
                    ctx.place(owner, ctx.newCreature(creatureCard, lane.getAsInt()), false, use.log());
                    return Optional.empty();
                }
            }
            return Optional.empty();
        }
        if (!p.deck().isEmpty() && p.deck().get(0) instanceof CreatureCard creatureCard) {
            p.deck().remove(0);
            ctx.place(owner, ctx.newCreature(creatureCard, lane.getAsInt()), true, use.log());
        }
        return Optional.empty();
    }
}
