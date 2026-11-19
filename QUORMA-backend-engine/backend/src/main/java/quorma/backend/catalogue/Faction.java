package quorma.backend.catalogue;

/**
 * The two factions, as the API and the database name them. The constant name ({@code RESISTENZA_UMANA}) is the
 * identifier in requests, responses and the {@code faction} columns. The frontend shows the label
 * {@code game.faction.<name in lower case>}.
 */
public enum Faction {
    POTENZE_ARTIFICIALI,
    RESISTENZA_UMANA;

    /** For {@code @Pattern} on request fields: must list every constant. */
    public static final String PATTERN = "POTENZE_ARTIFICIALI|RESISTENZA_UMANA";

    /** The other faction: the two players of a game always have opposite ones. */
    public Faction opposite() {
        return this == POTENZE_ARTIFICIALI ? RESISTENZA_UMANA : POTENZE_ARTIFICIALI;
    }
}
