package quorma.gameengine.model;

import java.util.List;
import java.util.Objects;
import quorma.gameengine.effects.BoundExpansion;

/**
 * Abilities an effect gave a creature for a while: a keyword (E14), an Evolution (E16), a static effect left by a
 * triggered one (a reduction of E28, a suppression of E18) or the effects of a spell that chose this creature.
 *
 * @param abilityId   the granted keyword or Evolution, or the ability of the effect that left it
 * @param expansions  the bound expansions the creature has while the grant lasts
 * @param lasting     how long it lasts
 * @param byEffect    true for a keyword or Evolution granted by an effect: E59 takes those away
 */
public record Grant(String abilityId, List<BoundExpansion> expansions, Lasting lasting, boolean byEffect) {

    public Grant {
        Objects.requireNonNull(abilityId, "abilityId");
        expansions = List.copyOf(expansions);
        Objects.requireNonNull(lasting, "lasting");
    }
}
