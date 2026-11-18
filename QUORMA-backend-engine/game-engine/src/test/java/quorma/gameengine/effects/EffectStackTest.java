package quorma.gameengine.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.RulesetLoader;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.FizzleReason;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.EffectFizzled;
import quorma.gameengine.log.MatchEvent.EffectResolved;
import quorma.gameengine.log.MatchEvent.EffectTriggered;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Ability;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.Lasting;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;

/**
 * The stack's order and its log: LIFO, fizzling, what may go on it, and the safety net. The hooks here are a minimal
 * stand-in for the turn's: an effect resolves on its fixed target through its {@link Effect} class and a
 * {@link FakeContext}; what the effects do is tested in {@link EffectClassesTest}. Insaziabile is +150 per mille of
 * base ATK ({@code balance.json}).
 */
class EffectStackTest {

    private static final AbilityBinder BINDER = new AbilityBinder(RulesetLoader.fromClasspath());

    private final List<MatchEvent> log = new ArrayList<>();
    private final EffectStack stack = new EffectStack(log, new TestHooks());

    /** What the stack asks of the turn code, reduced to what these tests need. */
    private static final class TestHooks implements EffectStack.Hooks {

        @Override
        public boolean allow(EffectStack.Item item) {
            if (!item.source().canTrigger(item.expansion())) {
                return false;
            }
            item.source().markTriggered(item.expansion());
            return true;
        }

        @Override
        public void resolve(EffectStack.Item item, EffectStack stack, List<MatchEvent> log) {
            BoundExpansion e = item.expansion().scaled(item.scalePermille());
            CreatureRef source = CreatureRef.of(item.side(), item.source());
            Creature target = item.target() != null ? item.target() : item.source();
            if (target.destroyed()) {
                log.add(new EffectFizzled(source, e.abilityId(), e.effectId(), FizzleReason.TARGET_DESTROYED));
                return;
            }
            Effect.Use use = new Effect.Use(e, item.side(), item.side(), item.source(), Lasting.permanent(),
                    item.context(), new Followups());
            Outcome outcome = Effects.find(e.effectId()).orElseThrow().apply(new FakeContext(), use, target);
            log.add(new EffectResolved(source, e.abilityId(), e.effectId(), CreatureRef.of(item.side(), target),
                    outcome.changes(), outcome.stacks()));
        }
    }

    private static Creature creature(String id, int lane, int atk, Ability... abilities) {
        CreatureCard card = new CreatureCard(id, 5, Suit.VINCOLO, Subfaction.ADATTATI, atk, 3000, List.of(abilities));
        return new Creature(card, lane, BINDER.bind(card));
    }

    private static Creature insaziabile(String id, int lane, int atk) {
        return creature(id, lane, atk, new AbilityRef("K_INSAZIABILE"));
    }

    private static BoundExpansion onKill(Creature c) {
        return c.abilities("T19").get(0);
    }

    private static EffectResolved grew(Creature c, int before, int after, int stacks) {
        CreatureRef ref = CreatureRef.of(Side.A, c);
        return new EffectResolved(ref, "K_INSAZIABILE", "E1", ref,
                List.of(new StatChange(StatChange.Stat.ATK, before, after)), OptionalInt.of(stacks));
    }

    @Test
    void lastInFirstOut() {
        Creature x = insaziabile("X", 1, 4000);
        Creature y = insaziabile("Y", 2, 2000);
        stack.push(Side.A, x, onKill(x));
        stack.push(Side.A, y, onKill(y));
        assertEquals(2, stack.size());

        stack.resolveAll();

        assertEquals(List.of(
                new EffectTriggered(CreatureRef.of(Side.A, x), "K_INSAZIABILE", "T19", "E1", 1),
                new EffectTriggered(CreatureRef.of(Side.A, y), "K_INSAZIABILE", "T19", "E1", 2),
                grew(y, 2000, 2300, 1),  // 2000 * 150 / 1000 = 300
                grew(x, 4000, 4600, 1)), // 4000 * 150 / 1000 = 600
                log);
        assertTrue(stack.isEmpty());
    }

    @Test
    void anEffectThatChangesNothingIsStillLoggedAsResolved() {
        Creature x = insaziabile("X", 1, 4000);
        for (int i = 0; i < 4; i++) {
            stack.push(Side.A, x, onKill(x));
        }
        stack.resolveAll();
        CreatureRef ref = CreatureRef.of(Side.A, x);
        assertEquals(new EffectResolved(ref, "K_INSAZIABILE", "E1", ref, List.of(), OptionalInt.of(3)),
                log.get(log.size() - 1), "the fourth Insaziabile, at max_stacks 3");
    }

    @Test
    void anEffectWhoseTargetIsDestroyedFizzles() {
        Creature x = insaziabile("X", 1, 4000);
        stack.push(Side.A, x, onKill(x));
        x.markDestroyed();
        stack.resolveAll();
        assertEquals(new EffectFizzled(CreatureRef.of(Side.A, x), "K_INSAZIABILE", "E1", FizzleReason.TARGET_DESTROYED),
                log.get(1));
        assertEquals(4000, x.currentAtk());
        assertEquals(Map.of(), x.stacks());
    }

    @Test
    void onlyTriggeredEffectsGoOnTheStack() {
        Creature x = creature("X", 1, 1000, new AbilityRef("K_TRABOCCANTE"));
        assertThrows(IllegalArgumentException.class, () -> stack.push(Side.A, x, x.abilities("T34").get(0)));
    }

    @Test
    void tooManyResolutionsStopTheSimulation() {
        EffectStack small = new EffectStack(log, 2, new TestHooks());
        Creature x = insaziabile("X", 1, 4000);
        for (int i = 0; i < 3; i++) {
            small.push(Side.A, x, onKill(x));
        }
        IllegalStateException e = assertThrows(IllegalStateException.class, small::resolveAll);
        assertTrue(e.getMessage().contains("did not empty after 2 resolutions"), e.getMessage());
        assertEquals(1000, EffectStack.MAX_RESOLUTIONS);
    }
}
