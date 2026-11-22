package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.effects.EffectStack;
import quorma.gameengine.effects.EvolutionFactorEffect;
import quorma.gameengine.effects.ForceEvolutionsEffect;
import quorma.gameengine.effects.RedirectAttackEffect;
import quorma.gameengine.effects.TriggerContext;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Side;

/**
 * Turns what happened ({@link GameEvent}s) into triggered effects on the stack. It decides which abilities listen,
 * whether their conditions hold now, and the order they are pushed in (ruleset 4.3); the stack and {@code Game} decide
 * the rest.
 */
final class TriggerDispatcher {

    private final MatchState state;
    private final Supplier<Board> board;

    TriggerDispatcher(MatchState state, Supplier<Board> board) {
        this.state = state;
        this.board = board;
    }

    private record Candidate(int sideRank, int lane, int seq, EffectStack.Item item) {}

    /**
     * Pushes the triggered abilities that listen to {@code events}, in ruleset 4.3 order: Potenze Artificiali
     * first, each side in lane order (the player's own abilities, such as suit tiers, after its creatures), a
     * creature's abilities in order. Conditions are checked now.
     */
    void fire(List<GameEvent> events, EffectStack stack) {
        Board b = board.get();
        List<Candidate> out = new ArrayList<>();
        Side[] order = b.prioritySides();
        for (GameEvent event : events) {
            switch (event) {
                case GameEvent.Destroyed d -> {
                    TriggerContext ctx = TriggerContext.destroyed(d.victim(), d.killerSide(), d.killer());
                    listen(b, out, order, d.side(), d.victim(), "T10", e -> true, ctx, true);
                    for (Creature c : state.player(d.side()).creatures()) {
                        if (c != d.victim()) {
                            listen(b, out, order, d.side(), c, "T36", e -> true, ctx, false);
                        }
                    }
                    for (Creature c : state.player(d.side().other()).creatures()) {
                        listen(b, out, order, d.side().other(), c, "T37", e -> true, ctx, false);
                    }
                    if (d.killer() != null) {
                        listen(b, out, order, d.killerSide(), d.killer(), "T19", e -> true, ctx, true);
                    }
                }
                case GameEvent.Damaged d -> {
                    TriggerContext ctx = TriggerContext.damagedBy(d.damagerSide(), d.damager(), d.amount());
                    listen(b, out, order, d.side(), d.victim(), "T13", e -> {
                        String source = e.triggerParam("source").orElse("qualsiasi");
                        return source.equals("qualsiasi") || source.equals(d.combat() ? "combattimento" : "effetto");
                    }, ctx, true);
                }
                case GameEvent.Dealt d -> listen(b, out, order, d.side(), d.attacker(), "T38", e -> true,
                        TriggerContext.dealt(d.victimSide(), d.victim(), d.amount(), d.killed()), true);
                case GameEvent.Survived s -> listen(b, out, order, s.side(), s.creature(), "T12", e -> true,
                        TriggerContext.NONE, false);
                case GameEvent.Spilled s -> listen(b, out, order, s.side(), s.attacker(), "T11", e -> true,
                        TriggerContext.NONE, true);
                case GameEvent.EvolutionFired f -> {
                    for (Creature c : state.player(f.side()).creatures()) {
                        listen(b, out, order, f.side(), c, "T15", e -> {
                            boolean ally = e.triggerParam("source").orElse("self").equals("ally");
                            return ally ? c != f.creature() : c == f.creature();
                        }, TriggerContext.NONE, false);
                    }
                }
                case GameEvent.SpellCast s -> {
                    for (Side side : order) {
                        for (Creature c : state.player(side).creatures()) {
                            listen(b, out, order, side, c, "T18", e -> {
                                String controller = e.triggerParam("controller").orElse("any");
                                boolean byController = controller.equals("any")
                                        || controller.equals(side == s.side() ? "own" : "opponent");
                                return byController && e.triggerParam("spell_id").map(id -> id.equals(s.cardId())).orElse(true);
                            }, TriggerContext.NONE, false);
                        }
                    }
                }
                case GameEvent.PhaseStart p -> {
                    for (Side side : order) {
                        for (Creature c : state.player(side).creatures()) {
                            listen(b, out, order, side, c, "T2", e -> e.triggerParam("phase").orElse("").equals(p.phase()),
                                    TriggerContext.NONE, false);
                        }
                    }
                }
                case GameEvent.EndOfTurn _ -> {
                    for (Side side : order) {
                        for (Creature c : state.player(side).creatures()) {
                            listen(b, out, order, side, c, "T26", e -> true, TriggerContext.NONE, false);
                        }
                    }
                }
                case GameEvent.Attacks a -> {
                    listen(b, out, order, a.side(), a.attacker(), "T6", e -> true,
                            TriggerContext.attacking(a.targetSide(), a.target(), a.side(), a.attacker()), false);
                    listen(b, out, order, a.targetSide(), a.target(), "T7", e -> true,
                            TriggerContext.attacking(a.targetSide(), a.target(), a.side(), a.attacker()), false);
                }
            }
        }
        out.sort(Comparator.comparingInt(Candidate::sideRank).thenComparingInt(Candidate::lane)
                .thenComparingInt(Candidate::seq));
        for (Candidate c : out) {
            stack.push(c.item());
        }
    }

    private void listen(Board b, List<Candidate> out, Side[] order, Side side, Creature source, String trigger,
            java.util.function.Predicate<BoundExpansion> params, TriggerContext ctx, boolean evenIfDestroyed) {
        if (source.destroyed() && !evenIfDestroyed) {
            return;
        }
        for (BoundExpansion e : b.abilities(source, trigger)) {
            if (e.effectId().equals(RedirectAttackEffect.ID) || e.effectId().equals(ForceEvolutionsEffect.ID) || !params.test(e)
                    || !b.holds(e, side, source, Board.Ctx.of(ctx))) {
                continue;
            }
            out.add(new Candidate(side == order[0] ? 0 : 1, source.lane(), out.size(),
                    EffectStack.Item.triggered(side, source, e, scaleOf(b, side, source, e), ctx)));
        }
    }

    /** Pushes {@code e}, an ability of {@code source}; its target is fixed now if it aims at the source (S1). */
    void push(EffectStack stack, Board b, Side side, Creature source, BoundExpansion e, TriggerContext ctx) {
        stack.push(EffectStack.Item.triggered(side, source, e, scaleOf(b, side, source, e), ctx));
    }

    /** The factor of the creature's E19 (Spira 4) for an Evolution, 1000 otherwise. */
    int scaleOf(Board b, Side side, Creature source, BoundExpansion e) {
        if (!e.evolution() || source.destroyed()) {
            return 1000;
        }
        long factor = 1000;
        for (Board.Active a : b.reaching(EvolutionFactorEffect.ID, source, Board.Ctx.NONE)) {
            factor = factor * a.e().intParam("factor") / 1000;
        }
        return Math.toIntExact(factor);
    }
}
