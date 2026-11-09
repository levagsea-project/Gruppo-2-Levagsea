package quorma.gameengine.model;

/** The eight subfactions (ruleset 2.2). {@link #dataName()} is the spelling used in {@code data/*.json}. */
public enum Subfaction {
    ADATTATI("Adattati", Faction.RESISTENZA_UMANA),
    SPEZZASIGILLI("Spezzasigilli", Faction.RESISTENZA_UMANA),
    PORTANOME("Portanome", Faction.RESISTENZA_UMANA),
    SEGNATERRA("Segnaterra", Faction.RESISTENZA_UMANA),
    SEKTAC("SEKTAC", Faction.POTENZE_ARTIFICIALI),
    MEROZYN("MEROZYN", Faction.POTENZE_ARTIFICIALI),
    EFFRIX("EFFRIX", Faction.POTENZE_ARTIFICIALI),
    YLMOR("YLMOR", Faction.POTENZE_ARTIFICIALI);

    private final String dataName;
    private final Faction faction;

    Subfaction(String dataName, Faction faction) {
        this.dataName = dataName;
        this.faction = faction;
    }

    /** Name as written in {@code vocab.json} and {@code subfactions.json}, for example {@code "Adattati"}. */
    public String dataName() {
        return dataName;
    }

    public Faction faction() {
        return faction;
    }

    public static Subfaction fromDataName(String name) {
        for (Subfaction s : values()) {
            if (s.dataName.equals(name)) {
                return s;
            }
        }
        throw new IllegalArgumentException("unknown subfaction '" + name + "'");
    }
}
