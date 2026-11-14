package quorma.gameengine;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import quorma.gameengine.input.MatchInput;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.model.Side;
import quorma.gameengine.turn.MatchReport;

/**
 * A played match, everything the MatchLog is made from.
 *
 * @param input        what was played
 * @param startingLife each player's life before turn 1
 * @param drawOrder    each deck after the initial shuffle, card ids top first (before setup draws)
 * @param setupEvents  ruleset 5: draws, cards set aside, reshuffles, player A first
 * @param report       every turn and the result
 */
public record Simulation(
        MatchInput input,
        int startingLife,
        Map<Side, List<String>> drawOrder,
        List<MatchEvent> setupEvents,
        MatchReport report) {

    public Simulation {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(report, "report");
        Map<Side, List<String>> orders = new EnumMap<>(Side.class);
        drawOrder.forEach((side, ids) -> orders.put(side, List.copyOf(ids)));
        drawOrder = Collections.unmodifiableMap(orders);
        setupEvents = List.copyOf(setupEvents);
    }
}
