package quorma.backend.telemetry;

/** One card of a deck in a game ({@code totals.cards[]}); {@code cardCode} is {@code cards.card_code}. */
public record CardTotals(Side side, String cardCode, long timesDrawn, long damageDealt, long spillDamage, long kills) {
}
