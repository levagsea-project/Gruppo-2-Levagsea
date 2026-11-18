package quorma.gameengine.input;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.Faction;
import quorma.gameengine.model.Side;

/**
 * What the engine needs to play one match ({@code schemas/match-input.schema.json}).
 *
 * @param rulesetVersion the ruleset the caller expects; the engine refuses another one
 * @param rulesetHash    the rule pack hash the caller expects, if it sent one
 */
public record MatchInput(
        int schemaVersion, long seed, String rulesetVersion, Optional<String> rulesetHash, Map<Side, Player> players) {

    /** The only schema version this engine reads. */
    public static final int SCHEMA_VERSION = 1;

    public MatchInput {
        Objects.requireNonNull(rulesetVersion, "rulesetVersion");
        Objects.requireNonNull(rulesetHash, "rulesetHash");
        if (players.size() != 2 || !players.containsKey(Side.A) || !players.containsKey(Side.B)) {
            throw new IllegalArgumentException("players must be exactly A and B, got " + players.keySet());
        }
        players = Collections.unmodifiableMap(new EnumMap<>(players)); // A then B, always
    }

    public Player player(Side side) {
        return players.get(side);
    }

    /**
     * One player's side of the input.
     *
     * @param poolSnapshotId opaque, echoed in the log header
     * @param deck           the deck before shuffling, in the order the caller sent it
     */
    public record Player(Faction faction, Optional<String> poolSnapshotId, List<Card> deck) {

        public Player {
            Objects.requireNonNull(faction, "faction");
            Objects.requireNonNull(poolSnapshotId, "poolSnapshotId");
            deck = List.copyOf(deck);
        }
    }
}
