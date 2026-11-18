package quorma.gameengine.effects;

import java.util.List;
import quorma.gameengine.model.Creature;

/** Helpers shared by the {@link Target} classes. */
final class TargetSupport {

    private TargetSupport() {}

    /** Adds {@code c} to {@code out} if it is there, not destroyed and still in play. */
    static void addLiving(List<Creature> out, Creature c) {
        if (c != null && !c.destroyed() && !c.leftPlay()) {
            out.add(c);
        }
    }
}
