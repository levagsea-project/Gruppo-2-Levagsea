package quorma.gameengine.effects;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The {@link Target} classes by vocabulary ID. Filled in the order of the ID numbers and never iterated for a result,
 * so the order is only for reading.
 */
public final class Targets {

    private static final Map<String, Target> BY_ID = register(
            new SelfTarget(),
            new TriggererTarget(),
            new LaneTarget(),
            new OpposingTarget(),
            new NextAdjacentTarget(),
            new OwnCreaturesTarget(),
            new EnemyCreaturesTarget(),
            new AllCreaturesTarget(),
            new SuitCreaturesTarget(),
            new OwnAttackersTarget(),
            new CombinationTarget(),
            new DamagerTarget(),
            new ReplacementTarget(),
            new OpponentPlayerTarget(),
            new OwnPlayerTarget(),
            new DiscardPileTarget(),
            new LowestLaneTarget(),
            new HighestAtkTarget(),
            new HighestDefTarget(),
            new LowestDefTarget(),
            new HighestRankTarget(),
            new LowestRankTarget(),
            new AdjacentAlliesTarget(),
            new DeclaredTargetTarget(),
            new DamagedNeighboursTarget(),
            new EnemyNeighboursTarget(),
            new DeckTopTarget());

    private Targets() {}

    /** The class for {@code targetId}, empty for an ID the engine does not implement. */
    public static Optional<Target> find(String targetId) {
        return Optional.ofNullable(BY_ID.get(targetId));
    }

    /** Every class, in the order they are registered. */
    public static Collection<Target> all() {
        return BY_ID.values();
    }

    private static Map<String, Target> register(Target... targets) {
        Map<String, Target> map = new LinkedHashMap<>();
        for (Target target : targets) {
            if (map.put(target.id(), target) != null) {
                throw new IllegalStateException("two classes for target " + target.id());
            }
        }
        return Collections.unmodifiableMap(map);
    }
}
