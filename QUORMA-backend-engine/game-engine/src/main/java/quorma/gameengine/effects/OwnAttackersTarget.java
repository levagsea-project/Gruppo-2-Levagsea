package quorma.gameengine.effects;

import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** S17: the creatures of the owner that attack this turn. */
public final class OwnAttackersTarget implements Target {

    @Override
    public String id() {
        return "S17";
    }

    @Override
    public boolean reaches(TargetView view, Side abilitySide, Creature source, Side side, Creature receiver) {
        return side == abilitySide && view.attacker(side, receiver);
    }
}
