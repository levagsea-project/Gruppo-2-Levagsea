package quorma.backend.catalogue;

/**
 * The eight subfactions, as the API and the {@code cards.subfaction} column name them. The constant name
 * ({@code ADATTATI}) is the identifier; the frontend shows the label {@code game.subfaction.<name in lower case>}.
 */
public enum Subfaction {
    ADATTATI(Faction.RESISTENZA_UMANA),
    SPEZZASIGILLI(Faction.RESISTENZA_UMANA),
    PORTANOME(Faction.RESISTENZA_UMANA),
    SEGNATERRA(Faction.RESISTENZA_UMANA),
    SEKTAC(Faction.POTENZE_ARTIFICIALI),
    MEROZYN(Faction.POTENZE_ARTIFICIALI),
    EFFRIX(Faction.POTENZE_ARTIFICIALI),
    YLMOR(Faction.POTENZE_ARTIFICIALI);

    private final Faction faction;

    Subfaction(Faction faction) {
        this.faction = faction;
    }

    public Faction faction() {
        return faction;
    }
}
