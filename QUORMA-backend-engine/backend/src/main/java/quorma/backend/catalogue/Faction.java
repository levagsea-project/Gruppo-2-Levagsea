package quorma.backend.catalogue;

/**
 * The two factions, as the API names them. The constant name ({@code RESISTENZA_UMANA}) is the identifier in
 * requests and responses. The frontend shows the label {@code game.faction.<name in lower case>}.
 */
public enum Faction {
    POTENZE_ARTIFICIALI("Potenze Artificiali"),
    RESISTENZA_UMANA("Resistenza Umana");

    /** For {@code @Pattern} on request fields: must list every constant. */
    public static final String PATTERN = "POTENZE_ARTIFICIALI|RESISTENZA_UMANA";

    private final String dataName;

    Faction(String dataName) {
        this.dataName = dataName;
    }

    /** Value of the {@code faction} columns in the database. */
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
