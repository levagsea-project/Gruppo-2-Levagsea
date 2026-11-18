package quorma.gameengine.effects;

import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/** S20: the creatures of the owner that form the combination of its hand. */
public final class CombinationTarget implements Target {

    @Override
    public String id() {
        return "S20";
    }

    @Override
    public boolean reaches(TargetView view, Side abilitySide, Creature source, Side side, Creature receiver) {
        return side == abilitySide && view.inCombination(side, receiver);
    }
}
