package quorma.gameengine.model;

/**
 * The two main factions. The declaration order is the resolution priority: Potenze Artificiali before
 * Resistenza Umana (ruleset 4.3 and 10.3). If both sides have the same faction, {@link Side#A} goes first.
 */
public enum Faction {
    POTENZE_ARTIFICIALI("Potenze Artificiali"),
    RESISTENZA_UMANA("Resistenza Umana");

    private final String dataName;

    Faction(String dataName) {
        this.dataName = dataName;
    }

    /** Name as written in {@code subfactions.json} and in the backend's {@code faction} columns. */
    public String dataName() {
        return dataName;
    }

    public static Faction fromDataName(String name) {
        for (Faction f : values()) {
            if (f.dataName.equals(name)) {
                return f;
            }
        }
        throw new IllegalArgumentException("unknown faction '" + name + "'");
    }
}
