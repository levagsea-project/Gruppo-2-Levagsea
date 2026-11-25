package quorma.backend.telemetry;

/**
 * What the engine counted for one suit synergy tier of one side in one game ({@code totals.synergies} of the match
 * log): the turns it was active at Evaluate, and the active turns in which it changed the outcome of a blow.
 */
public record SynergyTotals(Side side, String synergyId, long turnsActive, long decisiveTurns) {
}
