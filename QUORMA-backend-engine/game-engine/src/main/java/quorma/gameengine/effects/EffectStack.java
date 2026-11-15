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
 * and resolves next. An effect whose target is destroyed when it resolves fizzles. Every push, resolution and
 * fizzle is appended to the log. The stack only decides the order: what an effect does is
 * {@link EffectResolver}.</p>
 */
public final class EffectStack {

    /** Safety net: a stack that needs more resolutions than this is a loop in the data, an engine error. */
    public static final int MAX_RESOLUTIONS = 1000;

    private record Pending(Side side, Creature source, BoundExpansion expansion) {}

    private final Deque<Pending> stack = new ArrayDeque<>();
    private final List<MatchEvent> log;
    private final int maxResolutions;

    /** @param log where the stack appends its events */
    public EffectStack(List<MatchEvent> log) {
        this(log, MAX_RESOLUTIONS);
    }

    EffectStack(List<MatchEvent> log, int maxResolutions) {
        this.log = Objects.requireNonNull(log, "log");
        this.maxResolutions = maxResolutions;
    }

    /** Puts a triggered (class T) expansion of {@code source}, owned by {@code side}, on top of the stack. */
    public void push(Side side, Creature source, BoundExpansion expansion) {
        if (expansion.cls() != ResClass.T) {
            throw new IllegalArgumentException("only class T effects use the stack, got " + expansion.cls()
                    + " for " + expansion.abilityId() + " " + expansion.effectId());
        }
        stack.push(new Pending(side, source, expansion));
        log.add(new EffectTriggered(CreatureRef.of(side, source), expansion.abilityId(), expansion.triggerId(),
                expansion.effectId(), stack.size()));
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
            resolve(stack.pop());
        }
    }

    private void resolve(Pending top) {
        BoundExpansion e = top.expansion();
        CreatureRef source = CreatureRef.of(top.side(), top.source());
        // S1, the only target implemented: the source itself (EffectRegistry).
        Creature target = top.source();
        if (target.destroyed()) {
            log.add(new EffectFizzled(source, e.abilityId(), e.effectId(), FizzleReason.TARGET_DESTROYED));
            return;
        }
        EffectResolver.Outcome outcome = EffectResolver.resolve(e, target);
        log.add(new EffectResolved(source, e.abilityId(), e.effectId(), CreatureRef.of(top.side(), target),
                outcome.changes(), outcome.stacks()));
    }
}
