package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** S1: the creature whose ability it is. */
public final class SelfTarget implements Target {

    @Override
    public String id() {
        return TargetIds.SELF;
    }

    @Override
    public boolean single() {
        return true;
    }

    @Override
    public boolean reaches(TargetView view, Side abilitySide, Creature source, Side side, Creature receiver) {
        return source != null && receiver == source;
    }

    @Override
    public List<Creature> select(TargetView view, Side side, Creature source, TriggerContext ctx, BoundExpansion e) {
        List<Creature> out = new ArrayList<>();
        TargetSupport.addLiving(out, source);
        return out;
    }

    @Override
    public List<Creature> ofPlayerAbility(TargetView view, Side side, Creature source, BoundExpansion e) {
        return source != null && !source.destroyed() ? List.of(source) : List.of();
    }

    @Override
    public Optional<Creature> pick(TargetView view, BoundExpansion e, Side side, Creature source) {
        return Optional.ofNullable(source);
    }
}
