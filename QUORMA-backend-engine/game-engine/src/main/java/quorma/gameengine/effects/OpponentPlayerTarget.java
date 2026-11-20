package quorma.gameengine.effects;

/** S30: the other player. */
public final class OpponentPlayerTarget implements Target {

    @Override
    public String id() {
        return TargetIds.OPPONENT_PLAYER;
    }
}
