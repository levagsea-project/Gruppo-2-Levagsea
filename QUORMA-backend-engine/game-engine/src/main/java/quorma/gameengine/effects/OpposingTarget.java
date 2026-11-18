package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** S6: the enemy creature in the same lane as the source. */
public final class OpposingTarget implements Target {

    @Override
    public String id() {
        return "S6";
    }

    @Override
    public boolean single() {
        return true;
    }

    @Override
    public boolean reaches(TargetView view, Side abilitySide, Creature source, Side side, Creature receiver) {
        return source != null && side != abilitySide && receiver.lane() == source.lane();
    }

    @Override
    public List<Creature> select(TargetView view, Side side, Creature source, TriggerContext ctx, BoundExpansion e) {
        List<Creature> out = new ArrayList<>();
        view.creatureAt(side.other(), source.lane()).ifPresent(c -> TargetSupport.addLiving(out, c));
        return out;
    }

    @Override
    public Optional<Creature> pick(TargetView view, BoundExpansion e, Side side, Creature source) {
        return source == null ? Optional.empty()
                : view.creatureAt(side.other(), source.lane()).filter(x -> !x.destroyed());
    }
}
