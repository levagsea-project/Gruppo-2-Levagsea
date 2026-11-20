package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.List;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** S26: the creature that dealt the damage that triggered the effect. */
public final class DamagerTarget implements Target {

    @Override
    public String id() {
        return "S26";
    }

    @Override
    public boolean single() {
        return true;
    }

    @Override
    public List<Creature> select(TargetView view, Side side, Creature source, TriggerContext ctx, BoundExpansion e) {
        List<Creature> out = new ArrayList<>();
        TargetSupport.addLiving(out, ctx.damager());
        return out;
    }
}
