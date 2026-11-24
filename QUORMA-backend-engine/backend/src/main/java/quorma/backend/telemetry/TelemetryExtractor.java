package quorma.backend.telemetry;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/**
 * Reads the figures telemetry needs from an engine match log ({@code match-log.schema.json}): the outcome, the
 * factions, the {@code totals} the engine summed, and the hand type of every turn. It only reads numbers: it never
 * works out a result (the backend does not simulate).
 */
public final class TelemetryExtractor {

    private TelemetryExtractor() {
    }

    public static GameTelemetry from(UUID matchId, UUID snapshotIdA, UUID snapshotIdB, JsonNode log) {
        Result result;
        try {
            result = Result.valueOf(text(log.path("result"), "outcome"));
        } catch (IllegalArgumentException e) {
            throw new TelemetryException("the match log has an unknown outcome");
        }
        JsonNode players = log.path("header").path("players");
        JsonNode totals = log.path("totals");
        JsonNode sidesTotals = totals.path("players");

        Map<Side, Map<String, Integer>> hands = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            hands.put(side, new LinkedHashMap<>());
        }
        for (JsonNode turn : array(log, "turns")) {
            for (Side side : Side.values()) {
                hands.get(side).merge(text(turn.path("evaluation").path(side.name()), "hand_type"), 1, Integer::sum);
            }
        }

        List<CardTotals> cards = new ArrayList<>();
        for (JsonNode card : array(totals, "cards")) {
            cards.add(new CardTotals(side(card), text(card, "card_id"), number(card, "times_drawn"),
                    number(card, "damage_dealt"), number(card, "spill_damage"), number(card, "kills")));
        }

        return new GameTelemetry(matchId, snapshotIdA, snapshotIdB, result,
                text(players.path("A"), "faction"), text(players.path("B"), "faction"),
                sideTotals(sidesTotals, Side.A), sideTotals(sidesTotals, Side.B),
                hands.get(Side.A), hands.get(Side.B), cards);
    }

    private static SideTotals sideTotals(JsonNode players, Side side) {
        JsonNode node = players.path(side.name());
        return new SideTotals(number(node, "damage_dealt"), number(node, "spill_damage"), number(node, "kills"),
                number(node, "creatures_lost"));
    }

    private static Side side(JsonNode card) {
        try {
            return Side.valueOf(text(card, "side"));
        } catch (IllegalArgumentException e) {
            throw new TelemetryException("the match log has a card total with an unknown side");
        }
    }

    private static String text(JsonNode parent, String field) {
        JsonNode n = parent.path(field);
        if (!n.isString() || n.asString().isBlank()) {
            throw new TelemetryException("the match log has no usable " + field);
        }
        return n.asString();
    }

    private static long number(JsonNode parent, String field) {
        JsonNode n = parent.path(field);
        if (!n.isIntegralNumber() || n.asLong() < 0) {
            throw new TelemetryException("the match log has no usable " + field);
        }
        return n.asLong();
    }

    private static JsonNode array(JsonNode parent, String field) {
        JsonNode n = parent.path(field);
        if (!n.isArray()) {
            throw new TelemetryException("the match log has no " + field);
        }
        return n;
    }
}
