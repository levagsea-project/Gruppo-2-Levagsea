package quorma.gameengine.data;

import java.util.List;

/**
 * One tier of a suit synergy ({@code synergies.json}, ruleset 7.10).
 *
 * @param id        for example {@code VINCOLO_3}
 * @param suit      the {@code Suit} constant name, for example {@code VINCOLO}
 * @param tier      3, 4 or 5: the number of creatures of the suit that makes it active
 * @param perCreature true for {@code per_creature} (one copy of each expansion on every creature of the suit, the
 *                    creature being the source), false for {@code per_player} (the source is the player)
 * @param replaces  ids of the tiers that stop applying while this one is active
 * @param expandsTo what the tier does, in file order
 */
public record SynergyDef(String id, String suit, int tier, boolean perCreature, List<String> replaces, List<ExpansionDef> expandsTo) {

    public SynergyDef {
        replaces = List.copyOf(replaces);
        expandsTo = List.copyOf(expandsTo);
    }
}
