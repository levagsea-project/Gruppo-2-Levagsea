package quorma.gameengine.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.RulesetLoader;
import quorma.gameengine.data.Value;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Ability;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.Subfaction;

/**
 * What an effect does to its target, without the stack. Insaziabile (E1 {@code pct_base_atk}) is +150 per mille
 * of base ATK, at most 3 times ({@code balance.json}); values worked out by hand.
 */
class EffectResolverTest {

    private static final AbilityBinder BINDER = new AbilityBinder(RulesetLoader.fromClasspath());

    private static Creature creature(int atk, Ability... abilities) {
        CreatureCard card = new CreatureCard("X", 5, Suit.VINCOLO, Subfaction.ADATTATI, atk, 3000, List.of(abilities));
        return new Creature(card, 1, BINDER.bind(card));
    }

    private static EffectResolver.Outcome atk(int before, int after, int stacks) {
        return new EffectResolver.Outcome(
                List.of(new StatChange(StatChange.Stat.ATK, before, after)), OptionalInt.of(stacks));
    }

    @Test
    void insaziabileAddsAShareOfBaseAtkUpToMaxStacksThenChangesNothing() {
        Creature x = creature(4000, new AbilityRef("K_INSAZIABILE"));
        BoundExpansion e = x.abilities("T19").get(0);

        assertEquals(atk(4000, 4600, 1), EffectResolver.resolve(e, x));
        assertEquals(atk(4600, 5200, 2), EffectResolver.resolve(e, x), "600 again: a share of the base ATK");
        assertEquals(atk(5200, 5800, 3), EffectResolver.resolve(e, x));
        assertEquals(new EffectResolver.Outcome(List.of(), OptionalInt.of(3)), EffectResolver.resolve(e, x));
        assertEquals(5800, x.currentAtk());
        assertEquals(Map.of("K_INSAZIABILE", 3), x.stacks());
    }

    @Test
    void theBonusIsFloored() {
        Creature x = creature(1001, new AbilityRef("K_INSAZIABILE")); // 1001 * 150 / 1000 = 150.15
        EffectResolver.resolve(x.abilities("T19").get(0), x);
        assertEquals(1151, x.currentAtk());
    }

    @Test
    void anInlineGrowthWithoutStacksHasNoLimit() {
        ExpansionDef grow = new ExpansionDef("T19", Map.of(), "E1",
                Map.of("mode", new Value.StrV("pct_base_atk"), "permille", new Value.IntV(100)),
                "S1", Map.of(), "D_Permanente", List.of(), ResClass.T);
        Creature x = creature(1000, new InlineEffect(List.of(grow)));
        BoundExpansion e = x.abilities("T19").get(0);
        EffectResolver.Outcome last = null;
        for (int i = 0; i < 5; i++) {
            last = EffectResolver.resolve(e, x);
        }
        assertEquals(1500, x.currentAtk());
        assertEquals(OptionalInt.empty(), last.stacks());
        assertEquals(Map.of(), x.stacks());
    }

    @Test
    void aDestroyedCreatureIsNotAffected() {
        Creature x = creature(4000, new AbilityRef("K_INSAZIABILE"));
        x.markDestroyed();
        assertThrows(IllegalArgumentException.class, () -> EffectResolver.resolve(x.abilities("T19").get(0), x));
    }

    @Test
    void anEffectWithNoResolutionIsAnEngineBug() {
        // Scudo changes a blow's numbers in the damage code: it has no resolution here.
        Creature x = creature(4000, new AbilityRef("K_SCUDO", Map.of("x", new Value.IntV(25))));
        assertThrows(IllegalStateException.class, () -> EffectResolver.resolve(x.abilities("T0").get(0), x));
    }
}
