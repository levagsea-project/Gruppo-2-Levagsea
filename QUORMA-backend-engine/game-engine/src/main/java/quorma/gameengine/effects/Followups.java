package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.List;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.turn.GameEvent;

/**
 * What an effect leaves behind for its caller: log lines to append after the effect's own, then events to fire
 * (a creature destroyed by the effect triggers its destruction abilities).
 */
public record Followups(List<MatchEvent> logs, List<GameEvent> events) {

    public Followups() {
        this(new ArrayList<>(), new ArrayList<>());
    }
}
