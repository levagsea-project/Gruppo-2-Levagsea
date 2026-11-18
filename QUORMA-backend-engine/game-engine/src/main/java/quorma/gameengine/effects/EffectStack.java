package quorma.gameengine.effects;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.FizzleReason;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.EffectFizzled;
import quorma.gameengine.log.MatchEvent.EffectResolved;
import quorma.gameengine.log.MatchEvent.EffectTriggered;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/**
 * The stack of triggered effects.
 *
 * <p>The caller pushes the effects triggered together, already in ruleset 4.3 order, then calls
 * {@link #resolveAll()}. Effects resolve last in, first out; one triggered while another resolves goes on top
 * and resolves next. Every push, resolution and fizzle is appended to the log. The stack only decides the order:
 * what an effect does, and whether it may fire, is up to its {@link Hooks}.</p>
 */
public final class EffectStack {

    /** Safety net: a stack that needs more resolutions than this is a loop in the data, an engine error. */
    public static final int MAX_RESOLUTIONS = 1000;

    /**
     * One triggered effect.
     *
     * @param side          the side of its source
     * @param source        the creature whose ability it is (for a suit tier or a spell, the creature it affects)
     * @param target        the creature it affects when fixed at the push, null when the effect finds its targets when
     *                      it resolves
     * @param triggerId     what the log shows (T5 for an Evolution forced by Spira 5)
     * @param scalePermille multiplies the numbers of the effect when it resolves (E19, Spira 4)
     * @param context       what the trigger provides
     */
    public record Item(Side side, Creature source, Creature target, BoundExpansion expansion, String triggerId,
            int scalePermille, TriggerContext context) {

        public Item {
            Objects.requireNonNull(side, "side");
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(expansion, "expansion");
            Objects.requireNonNull(context, "context");
        }
    }

    /** What the turn code decides about the effects on the stack. */
    public interface Hooks {

        /** True if {@code item} may be pushed; it is then counted as fired. */
        boolean allow(Item item);

        /** Called once {@code item} is on the stack (an Evolution that fired triggers T15). */
        default void pushed(Item item, EffectStack stack) {}

        /** Resolves {@code item}, appending to {@code log}. Effects it triggers go on {@code stack}. */
        void resolve(Item item, EffectStack stack, List<MatchEvent> log);
    }

    /**
     * The hooks of the stack alone: an Evolution fires once per creature, and an effect resolves on its fixed target
     * through {@link EffectResolver}, fizzling when the target is destroyed.
     */
    public static final Hooks BASIC = new Hooks() {
        @Override
        public boolean allow(Item item) {
            if (!item.source().canTrigger(item.expansion())) {
                return false;
            }
            item.source().markTriggered(item.expansion());
            return true;
        }

        @Override
        public void resolve(Item item, EffectStack stack, List<MatchEvent> log) {
            BoundExpansion e = item.expansion();
            CreatureRef source = CreatureRef.of(item.side(), item.source());
            Creature target = item.target() != null ? item.target() : item.source();
            if (target.destroyed()) {
                log.add(new EffectFizzled(source, e.abilityId(), e.effectId(), FizzleReason.TARGET_DESTROYED));
                return;
            }
            EffectResolver.Outcome outcome = EffectResolver.resolve(e.scaled(item.scalePermille()), target);
            log.add(new EffectResolved(source, e.abilityId(), e.effectId(), CreatureRef.of(item.side(), target),
                    outcome.changes(), outcome.stacks()));
        }
    };

    private final Deque<Item> stack = new ArrayDeque<>();
    private final List<MatchEvent> log;
    private final int maxResolutions;
    private final Hooks hooks;

    /** @param log where the stack appends its events */
    public EffectStack(List<MatchEvent> log) {
        this(log, BASIC);
    }

    public EffectStack(List<MatchEvent> log, Hooks hooks) {
        this(log, MAX_RESOLUTIONS, hooks);
    }

    EffectStack(List<MatchEvent> log, int maxResolutions) {
        this(log, maxResolutions, BASIC);
    }

    private EffectStack(List<MatchEvent> log, int maxResolutions, Hooks hooks) {
        this.log = Objects.requireNonNull(log, "log");
        this.maxResolutions = maxResolutions;
        this.hooks = Objects.requireNonNull(hooks, "hooks");
    }

    /** The log the stack appends to. */
    public List<MatchEvent> log() {
        return log;
    }

    /** Puts a triggered (class T) expansion of {@code source}, owned by {@code side}, on top of the stack. */
    public void push(Side side, Creature source, BoundExpansion expansion) {
        push(side, source, source, expansion);
    }

    /**
     * Puts a triggered expansion on top of the stack with a target other than its source: a suit synergy tier whose
     * source is the player is logged with the creature it affects as source.
     */
    public void push(Side side, Creature source, Creature target, BoundExpansion expansion) {
        push(side, source, target, expansion, expansion.triggerId(), 1000);
    }

    /**
     * The full form with a fixed target. An effect its hooks refuse (an Evolution that already fired) is not pushed.
     * {@code triggerId} is what the log shows (T5 for an Evolution forced by Spira 5). {@code scalePermille}
     * multiplies the numbers of the effect when it resolves (E19, Spira 4).
     */
    public void push(Side side, Creature source, Creature target, BoundExpansion expansion, String triggerId,
            int scalePermille) {
        push(new Item(side, source, target, expansion, triggerId, scalePermille, TriggerContext.NONE));
    }

    /** Pushes {@code item} if its hooks allow it. */
    public void push(Item item) {
        if (item.expansion().cls() != ResClass.T) {
            throw new IllegalArgumentException("only class T effects use the stack, got " + item.expansion().cls()
                    + " for " + item.expansion().abilityId() + " " + item.expansion().effectId());
        }
        if (!hooks.allow(item)) {
            return;
        }
        stack.push(item);
        log.add(new EffectTriggered(CreatureRef.of(item.side(), item.source()), item.expansion().abilityId(),
                item.triggerId(), item.expansion().effectId(), stack.size()));
        hooks.pushed(item, this);
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public int size() {
        return stack.size();
    }

    /**
     * Resolves the top of the stack until it is empty.
     *
     * @throws IllegalStateException after {@link #MAX_RESOLUTIONS} resolutions
     */
    public void resolveAll() {
        int resolutions = 0;
        while (!stack.isEmpty()) {
            if (resolutions++ == maxResolutions) {
                throw new IllegalStateException(
                        "the effect stack did not empty after " + maxResolutions + " resolutions: a loop in the abilities");
            }
            hooks.resolve(stack.pop(), this, log);
        }
    }
}
