package quorma.gameengine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import quorma.gameengine.testkit.TurnResolvers;

/** The data names are what {@code subfactions.json} and the backend's {@code faction} columns store. */
class FactionTest {

    @Test
    void dataNamesRoundTrip() {
        for (Faction f : Faction.values()) {
            assertEquals(f, Faction.fromDataName(f.dataName()));
        }
        assertEquals("Resistenza Umana", Faction.RESISTENZA_UMANA.dataName());
        assertEquals("Potenze Artificiali", Faction.POTENZE_ARTIFICIALI.dataName());
    }

    @Test
    void anUnknownNameFails() {
        assertThrows(IllegalArgumentException.class, () -> Faction.fromDataName("resistenza umana"));
    }

    @Test
    void everySubfactionBelongsToTheFactionTheDataFilesNameForIt() {
        var rules = TurnResolvers.RULES;
        for (Subfaction s : Subfaction.values()) {
            assertEquals(s.faction(), Faction.fromDataName(rules.subfactions().get(s.dataName()).faction()), s.name());
        }
    }
}
