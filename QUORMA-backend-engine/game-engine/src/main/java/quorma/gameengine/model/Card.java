package quorma.gameengine.model;

import java.util.List;

/** A card in a deck. Decks hold creatures, spells and equipment (ruleset 3). */
public sealed interface Card permits CreatureCard, SpellCard, EquipmentCard {

    String id();

    Faction faction();

    /**
     * What the card can do, in the order they are written on the card (several keywords on one card resolve in
     * list order). The same for every kind of card. May be empty.
     */
    List<Ability> abilities();
}
