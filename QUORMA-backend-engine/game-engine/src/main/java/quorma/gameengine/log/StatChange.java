package quorma.gameengine.log;

import java.util.Objects;

/** A stat of a creature changed by an effect, before and after. */
public record StatChange(Stat stat, int before, int after) {

    public enum Stat { ATK, DEF }

    public StatChange {
        Objects.requireNonNull(stat, "stat");
    }
}
