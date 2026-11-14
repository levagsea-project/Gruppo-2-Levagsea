package quorma.gameengine.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;

/** Both sides of the table at one moment, as the MatchLog shows them: values only, no live state. */
public record BoardSnapshot(Map<Side, PlayerBoard> sides) {

    public BoardSnapshot {
        sides = Collections.unmodifiableMap(new EnumMap<>(sides));
        if (sides.size() != 2) {
            throw new IllegalArgumentException("a board has sides A and B, got " + sides.keySet());
        }
    }

    public PlayerBoard side(Side side) {
        return sides.get(side);
    }

    /** The board of {@code state} now. */
    public static BoardSnapshot of(MatchState state) {
        Map<Side, PlayerBoard> sides = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            PlayerState p = state.player(side);
            List<Optional<LaneState>> lanes = new ArrayList<>();
            for (int lane = 1; lane <= PlayerState.LANES; lane++) {
                lanes.add(p.creatureAt(lane).map(LaneState::of));
            }
            sides.put(side, new PlayerBoard(p.life(), p.deck().size(), p.discard().size(), lanes));
        }
        return new BoardSnapshot(sides);
    }

    /** @param lanes exactly 5, lane 1 first; empty for an empty lane */
    public record PlayerBoard(int life, int deckCount, int discardCount, List<Optional<LaneState>> lanes) {

        public PlayerBoard {
            lanes = List.copyOf(lanes);
            if (lanes.size() != PlayerState.LANES) {
                throw new IllegalArgumentException("a board has " + PlayerState.LANES + " lanes, got " + lanes.size());
            }
        }

        /** The card id in {@code lane} (1..5), if any. */
        public Optional<String> cardId(int lane) {
            return lanes.get(lane - 1).map(LaneState::cardId);
        }
    }

    /**
     * @param atk    current ATK, modifiers included, before the subfaction advantage
     * @param stacks applied stacks per stack key, in the order each key was first applied
     */
    public record LaneState(String cardId, int atk, int def, boolean destroyed, Map<String, Integer> stacks) {

        public LaneState {
            Objects.requireNonNull(cardId, "cardId");
            stacks = Collections.unmodifiableMap(new LinkedHashMap<>(stacks));
        }

        static LaneState of(Creature c) {
            return new LaneState(c.card().id(), c.currentAtk(), c.currentDef(), c.destroyed(), c.stacks());
        }
    }
}
