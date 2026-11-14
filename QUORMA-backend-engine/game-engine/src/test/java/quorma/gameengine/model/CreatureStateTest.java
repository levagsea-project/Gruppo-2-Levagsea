package quorma.gameengine.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.RulesetLoader;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.hand.Suit;

/** The state a creature gains from its abilities: bound expansions, ATK bonus and stack counts. */
class CreatureStateTest {

    private static CreatureCard card(int atk, List<Ability> abilities) {
        return new CreatureCard("C", 5, Suit.SPIRA, Subfaction.SEKTAC, atk, 3000, abilities);
    }

    @Test
    void atkBonusAddsToThePrintedAtkAndNeverGoesBelowZero() {
        Creature c = new Creature(card(4000, List.of()), 1);
        c.addAtk(600);
        c.addAtk(600);
        assertEquals(5200, c.currentAtk());
        c.addAtk(-6000);
        assertEquals(0, c.currentAtk(), "ruleset 9.1: ATK never below 0");
        c.addAtk(1000);
        assertEquals(200, c.currentAtk(), "the bonus is kept exactly, the clamp is only on the result");
    }

    @Test
    void stacksCountPerKeyInFirstUseOrder() {
        Creature c = new Creature(card(1000, List.of()), 1);
        assertEquals(0, c.stacks("K_INSAZIABILE"));
        assertEquals(1, c.addStack("K_INSAZIABILE"));
        assertEquals(1, c.addStack("K_OTHER"));
        assertEquals(2, c.addStack("K_INSAZIABILE"));
        assertEquals(List.of("K_INSAZIABILE", "K_OTHER"), List.copyOf(c.stacks().keySet()));
        assertEquals(Map.of("K_INSAZIABILE", 2, "K_OTHER", 1), c.stacks());
        assertThrows(UnsupportedOperationException.class, () -> c.stacks().put("x", 1));
    }

    @Test
    void boundAbilitiesAreFilteredByTrigger() {
        CreatureCard card = card(4000, List.of(
                new AbilityRef("K_INSAZIABILE"), new AbilityRef("K_TRABOCCANTE"), new AbilityRef("K_INSAZIABILE")));
        Creature c = new Creature(card, 1, new AbilityBinder(RulesetLoader.fromClasspath()).bind(card));
        assertEquals(3, c.abilities().size());
        assertEquals(List.of("K_INSAZIABILE", "K_INSAZIABILE"),
                c.abilities("T19").stream().map(BoundExpansion::abilityId).toList());
        assertEquals(List.of(), c.abilities("T0"));
        assertEquals(List.of(), new Creature(card, 1).abilities(), "the two-argument constructor binds nothing");
    }
}
