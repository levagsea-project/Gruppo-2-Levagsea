package quorma.gameengine.effects;

/** Where an expansion is written: what the engine does with it depends on it. */
public enum Origin {

    /** An ability of a creature: a keyword, an Evolution or an ability written on the card; also a suit synergy tier. */
    CREATURE,

    /** A spell: immediate effects, and lasting ones that stay with the player or go to a chosen creature. */
    SPELL,

    /** An equipment: immediate effects, and lasting ones that go to the creature it is applied to. */
    EQUIPMENT
}
