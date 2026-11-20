package quorma.gameengine.effects;

import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** S45: the enemy creatures in the source's lane and the lanes next to it. */
public final class EnemyNeighboursTarget implements Target {

    @Override
    public String id() {
        return "S45";
    }

    @Override
    public boolean reaches(TargetView view, Side abilitySide, Creature source, Side side, Creature receiver) {
        return source != null && side != abilitySide && Math.abs(receiver.lane() - source.lane()) <= 1;
    }
}
