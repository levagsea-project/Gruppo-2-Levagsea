package quorma.gameengine.effects;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import quorma.gameengine.data.Value;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/**
 * What a {@link Target} may read to find its creatures: the table at one moment. The turn's {@code Board} implements it;
 * only what targets need is here.
 */
public interface TargetView {

    /** The creatures of {@code side} in lane order, destroyed ones included until Refill. */
    List<Creature> creatures(Side side);

    /** The creature of {@code side} in {@code lane}, if any (it may be destroyed). */
    Optional<Creature> creatureAt(Side side, int lane);

    /** Every creature in play, in lane order, Potenze Artificiali first within a lane. */
    List<Creature> everyone();

    /** Ruleset 7.10.4: whether two creatures of {@code side} are adjacent (E34 links and E60 included). */
    boolean adjacent(Side side, Creature a, Creature b);

    /** The living creatures of {@code side} in the lanes next to {@code creature} (lanes differing by 1). */
    List<Creature> neighbours(Side side, Creature creature);

    /** True if {@code creature} of {@code side} attacks this turn. */
    boolean attacker(Side side, Creature creature);

    /** True if {@code creature} is one of the creatures forming its player's combination (S20). */
    boolean inCombination(Side side, Creature creature);

    /** {@code target_params.filters}, seen from {@code controller}, on {@code creature} of {@code side}. */
    boolean filtersHold(List<String> filters, Side controller, Side side, Creature creature);

    /** 0 for a creature of Potenze Artificiali, 1 for the other faction: the tie-break of selections. */
    int factionRank(Creature creature);

    /**
     * The creature a ranked target ({@link RankedTarget}) picks among the living creatures of both sides that pass the
     * filters in {@code params}, seen from {@code controller}.
     *
     * @throws IllegalStateException if {@code targetId} is not a ranked target
     */
    Optional<Creature> select(String targetId, Map<String, Value> params, Side controller);
}
