package quorma.backend.telemetry;

/** Sums of one player's events in a game ({@code totals.players.<side>} of the match log). */
public record SideTotals(long damageDealt, long spillDamage, long kills, long creaturesLost) {
}
