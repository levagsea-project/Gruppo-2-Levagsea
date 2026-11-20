package quorma.gameengine.effects;

/** S31: the ability's owner, as a player. */
public final class OwnPlayerTarget implements Target {

    @Override
    public String id() {
        return TargetIds.OWN_PLAYER;
    }
}
