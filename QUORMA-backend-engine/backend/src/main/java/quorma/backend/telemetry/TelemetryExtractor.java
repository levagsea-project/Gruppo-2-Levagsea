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

    /** The event of a series: every game's log read in order. */
    public static SeriesFinishedEvent series(UUID seriesId, UUID playerAId, UUID playerBId, Result seriesResult,
                                             List<GameLog> games) {
        return new SeriesFinishedEvent(seriesId, playerAId, playerBId, seriesResult, games.stream()
                .map(g -> from(g.matchId(), g.snapshotIdA(), g.snapshotIdB(), g.log()))
                .toList());
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

        // Logs of an engine before 1.0.0-alpha.1 have neither turns_total nor synergies: no synergy figures, the rest still counts.
        List<SynergyTotals> synergies = new ArrayList<>();
        long turnsTotal = 0;
        if (totals.has("synergies")) {
            turnsTotal = number(totals, "turns_total");
            for (JsonNode s : array(totals, "synergies")) {
                SynergyTotals entry = new SynergyTotals(side(s), text(s, "synergy_id"), number(s, "turns_active"),
                        number(s, "decisive_turns"));
                if (entry.turnsActive() > turnsTotal || entry.decisiveTurns() > entry.turnsActive()) {
                    throw new TelemetryException("the match log has inconsistent figures for " + entry.synergyId());
                }
                synergies.add(entry);
            }
        }

        return new GameTelemetry(matchId, snapshotIdA, snapshotIdB, result,
                text(players.path("A"), "faction"), text(players.path("B"), "faction"),
                sideTotals(sidesTotals, Side.A), sideTotals(sidesTotals, Side.B),
                hands.get(Side.A), hands.get(Side.B), cards,
                synergies.isEmpty() ? "" : text(log.path("header"), "ruleset_version"), turnsTotal, synergies);
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
            throw new TelemetryException("the match log has a total with an unknown side");
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
