package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** S11: every creature in play. */
public final class AllCreaturesTarget implements Target {

    @Override
    public String id() {
        return TargetIds.ALL_CREATURES;
    }

    @Override
    public boolean reaches(TargetView view, Side abilitySide, Creature source, Side side, Creature receiver) {
        return true;
    }

    @Override
    public List<Creature> select(TargetView view, Side side, Creature source, TriggerContext ctx, BoundExpansion e) {
        List<Creature> out = new ArrayList<>();
        view.everyone().forEach(c -> TargetSupport.addLiving(out, c));
        return out;
    }

    @Override
    public List<Creature> selectImmediate(TargetView view, BoundExpansion e, Side owner, Creature receiver,
            Map<String, Creature> chosen) {
        List<Creature> out = new ArrayList<>();
        view.everyone().forEach(c -> TargetSupport.addLiving(out, c));
        return out;
    }
}
