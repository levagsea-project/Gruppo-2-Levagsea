package quorma.gameengine.model;

/**
 * The two main factions. The declaration order is the resolution priority: Potenze Artificiali before
 * Resistenza Umana (ruleset 4.3 and 10.3). If both sides have the same faction, {@link Side#A} goes first.
 */
public enum Faction {
    POTENZE_ARTIFICIALI,
    RESISTENZA_UMANA
}
