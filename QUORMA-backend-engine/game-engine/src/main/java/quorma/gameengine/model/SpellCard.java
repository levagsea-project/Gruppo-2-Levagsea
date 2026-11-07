package quorma.gameengine.model;

import java.util.List;
import java.util.Objects;

/**
 * A spell (ruleset 3.3). Its abilities are written like any other card's: references to keywords, or whole
 * effects. It takes effect when drawn (ruleset 10.5).
 */
public record SpellCard(String id, Faction faction, List<Ability> abilities) implements Card {

    public SpellCard {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(faction, "faction");
        abilities = List.copyOf(abilities);
    }

    /** A spell with no abilities, for tests. */
    public SpellCard(String id, Faction faction) {
        this(id, faction, List.of());
    }
}
