package quorma.gameengine.effects;

import java.util.Comparator;
import java.util.List;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/**
 * A target that picks the one creature ranking first among the living creatures of both sides that pass its filters
 * (S33, S35, S37, S38, S39, S40). The ties are part of each target's description: lowest lane, Potenze Artificiali
 * first, then rank, then suit order.
 */
public interface RankedTarget extends Target {

    /** The ranking: the creature that sorts first is the one picked. */
    Comparator<Creature> order(TargetView view);

    @Override
    default List<Creature> select(TargetView view, Side side, Creature source, TriggerContext ctx, BoundExpansion e) {
        return view.select(id(), e.targetParams(), side).map(List::of).orElse(List.of());
    }

    /** Rank value for comparisons: Ace high (14). */
    static int rankValue(Creature c) {
        return c.card().rank() == 1 ? 14 : c.card().rank();
    }

    static Comparator<Creature> byLane() {
        return Comparator.comparingInt(Creature::lane);
    }

    static Comparator<Creature> byFaction(TargetView view) {
        return Comparator.comparingInt(view::factionRank);
    }

    static Comparator<Creature> rankHigh() {
        return Comparator.comparingInt((Creature c) -> -rankValue(c));
    }

    static Comparator<Creature> rankLow() {
        return Comparator.comparingInt(RankedTarget::rankValue);
    }

    static Comparator<Creature> bySuit() {
        return Comparator.comparingInt(c -> c.card().suit().ordinal());
    }
}
