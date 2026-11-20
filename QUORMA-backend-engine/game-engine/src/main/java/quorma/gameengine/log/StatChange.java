package quorma.gameengine.log;

import java.util.Objects;

/** A stat of a creature (ATK, DEF) or of a player (LIFE) changed by an effect, before and after. */
public record StatChange(Stat stat, int before, int after) {

    public enum Stat { ATK, DEF, LIFE }

    public StatChange {
        Objects.requireNonNull(stat, "stat");
    }
}
