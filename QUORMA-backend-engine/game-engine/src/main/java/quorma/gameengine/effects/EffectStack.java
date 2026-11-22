package quorma.gameengine.effects;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.MatchEvent;
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

        /**
         * An ability of {@code source} that fired on its own trigger. An effect aimed at the source itself (S1) has
         * its target fixed now; any other finds its targets when it resolves.
         */
        public static Item triggered(Side side, Creature source, BoundExpansion expansion, int scalePermille,
                TriggerContext context) {
            return triggered(side, source, expansion, expansion.triggerId(), scalePermille, context);
        }

        /** Like {@link #triggered(Side, Creature, BoundExpansion, int, TriggerContext)} with another trigger shown in the log. */
        public static Item triggered(Side side, Creature source, BoundExpansion expansion, String triggerId,
                int scalePermille, TriggerContext context) {
            Creature fixed = expansion.targetId().equals(TargetIds.SELF) ? source : null;
            return new Item(side, source, fixed, expansion, triggerId, scalePermille, context);
        }

        /** An effect of the player or of a spell that affects {@code creature}, which is also its source (no scaling). */
        public static Item onCreature(Side side, Creature creature, BoundExpansion expansion, String triggerId,
                TriggerContext context) {
            return new Item(side, creature, creature, expansion, triggerId, 1000, context);
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

    private final Deque<Item> stack = new ArrayDeque<>();
    private final List<MatchEvent> log;
    private final int maxResolutions;
    private final Hooks hooks;

    /**
     * @param log   where the stack appends its events
     * @param hooks what the caller decides: whether an effect may fire and what it does when it resolves
     */
    public EffectStack(List<MatchEvent> log, Hooks hooks) {
        this(log, MAX_RESOLUTIONS, hooks);
    }

    EffectStack(List<MatchEvent> log, int maxResolutions, Hooks hooks) {
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
