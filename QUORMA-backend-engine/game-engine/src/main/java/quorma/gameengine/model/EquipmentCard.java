package quorma.gameengine.model;

import java.util.List;
import java.util.Objects;

/**
 * A piece of equipment (ruleset 3.4). Its abilities are written like any other card's: references to keywords,
 * or whole effects. It applies to the next creature drawn (ruleset 10.6).
 */
public record EquipmentCard(String id, Faction faction, List<Ability> abilities) implements Card {

    public EquipmentCard {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(faction, "faction");
        abilities = List.copyOf(abilities);
    }

    /** An equipment with no abilities, for tests. */
    public EquipmentCard(String id, Faction faction) {
        this(id, faction, List.of());
    }
}
