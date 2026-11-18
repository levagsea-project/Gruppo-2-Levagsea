package quorma.gameengine;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import quorma.gameengine.input.MatchInput;
import quorma.gameengine.log.BoardSnapshot;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.model.Side;
import quorma.gameengine.turn.MatchReport;

/**
 * A played match, everything the MatchLog is made from.
 *
 * @param engineVersion  the engine that played it ({@link EngineVersion})
 * @param rulesetVersion the ruleset it was played on, for example {@code 1.0-alpha}
 * @param rulesetHash    the hash of the rule pack it was played on
 * @param input          what was played
 * @param startingLife   each player's life before turn 1
 * @param drawOrder      each deck after the initial shuffle, card ids top first (before setup draws)
 * @param setupEvents    ruleset 5: draws, cards set aside, reshuffles, player A first
 * @param setupBoard     the table after setup, before turn 1
 * @param report         every turn and the result
 */
public record Simulation(
        String engineVersion,
        String rulesetVersion,
        String rulesetHash,
        MatchInput input,
        int startingLife,
        Map<Side, List<String>> drawOrder,
        List<MatchEvent> setupEvents,
        BoardSnapshot setupBoard,
        MatchReport report) {

    public Simulation {
        Objects.requireNonNull(engineVersion, "engineVersion");
        Objects.requireNonNull(rulesetVersion, "rulesetVersion");
        Objects.requireNonNull(rulesetHash, "rulesetHash");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(setupBoard, "setupBoard");
        Objects.requireNonNull(report, "report");
        Map<Side, List<String>> orders = new EnumMap<>(Side.class);
        drawOrder.forEach((side, ids) -> orders.put(side, List.copyOf(ids)));
        drawOrder = Collections.unmodifiableMap(orders);
        setupEvents = List.copyOf(setupEvents);
    }

    /** The canonical MatchLog JSON of this match ({@code schemas/match-log.schema.json}). */
    public String toMatchLog() {
        return MatchLogWriter.write(this);
    }
}
