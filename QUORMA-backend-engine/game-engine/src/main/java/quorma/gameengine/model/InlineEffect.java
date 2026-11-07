package quorma.gameengine.model;

import java.util.List;
import quorma.gameengine.data.ExpansionDef;

/**
 * A whole effect written on the card instead of referenced by id: one or more expansions (trigger, effect,
 * target), the same type the data files use. Spells and equipment are the usual users, with class I
 * expansions that resolve when the card is drawn (ruleset 10.5 and 10.6), but any card may have one.
 */
public record InlineEffect(List<ExpansionDef> expansions) implements Ability {

    public InlineEffect {
        expansions = List.copyOf(expansions);
        if (expansions.isEmpty()) {
            throw new IllegalArgumentException("an inline effect needs at least one expansion");
        }
    }
}
