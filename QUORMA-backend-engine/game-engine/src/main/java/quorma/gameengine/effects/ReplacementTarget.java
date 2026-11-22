package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** S27: the creature the trigger placed (a replacement, or the receiver of an equipment). */
public final class ReplacementTarget implements Target {

    @Override
    public String id() {
        return TargetIds.REPLACEMENT;
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
        TargetSupport.addLiving(out, ctx.replacement());
        return out;
    }

    @Override
    public List<Creature> selectImmediate(TargetView view, BoundExpansion e, Side owner, Creature receiver,
            Map<String, Creature> chosen) {
        List<Creature> out = new ArrayList<>();
        if (receiver != null && !receiver.destroyed()) {
            out.add(receiver);
        }
        return out;
    }
}
