package quorma.gameengine.model;

import java.util.Objects;

/** A creature an attack aims at, identified by its owner and lane (targets are frozen at Declare). */
public record Target(Side side, int lane) {

    public Target {
        Objects.requireNonNull(side, "side");
        if (lane < 1 || lane > PlayerState.LANES) {
            throw new IllegalArgumentException("lane must be 1.." + PlayerState.LANES + ", got " + lane);
        }
    }
}
