package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.Value;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.effects.AbilityLayer;
import quorma.gameengine.effects.AbilityLayerEffect;
import quorma.gameengine.effects.AdjacencyLinkEffect;
import quorma.gameengine.effects.AtkEffect;
import quorma.gameengine.effects.BoundExpansion;
import quorma.gameengine.effects.Condition;
import quorma.gameengine.effects.ConditionContext;
import quorma.gameengine.effects.Conditions;
import quorma.gameengine.effects.EffectDefinitions;
import quorma.gameengine.effects.HealFactorEffect;
import quorma.gameengine.effects.IsolatedEffect;
import quorma.gameengine.effects.LethalDamageEffect;
import quorma.gameengine.effects.NoAtkReductionEffect;
import quorma.gameengine.effects.NoBonusEffect;
import quorma.gameengine.effects.RankStepEffect;
import quorma.gameengine.effects.RankedTarget;
import quorma.gameengine.effects.SuppressEvolutionEffect;
import quorma.gameengine.effects.DamageReductionEffect;
import quorma.gameengine.effects.TargetIds;
import quorma.gameengine.effects.TargetView;
import quorma.gameengine.effects.Targets;
import quorma.gameengine.effects.TriggerContext;
import quorma.gameengine.effects.WildSuitEffect;
import quorma.gameengine.hand.HandType;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.Modifier;
import quorma.gameengine.model.Aura;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Faction;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Side;

/**
 * The table as the effects see it at one moment: what each creature can do once the static effects of every
 * creature and spell are applied, who those effects reach, and whether conditions hold. A {@code Board} is a view:
 * build a new one after the state changes (it keeps what it worked out).
 *
 * <p>Abilities come in two layers. The first is what a creature has on its own ({@link Creature#abilities()}: card,
 * equipment, grants, suit tiers). The second adds the keywords and Evolutions that static effects grant it (E14, E16
 * with trigger T0) and takes away the keywords static effects remove (E15). Static effects that grant or remove are
 * read on the first layer, so a granted keyword never grants another.</p>
 *
 * <p>A destroyed creature neither gives nor receives static effects (ruleset 7.10.3).</p>
 */
final class Board implements TargetView {

    /** A static effect in force: from a creature ({@code source}) or from a spell of {@code side} ({@code aura}). */
    record Active(Side side, Creature source, BoundExpansion e, Aura aura) {

        /** The creature the effect is logged as coming from: its source, or the creature it reaches for a spell. */
        Creature sourceOr(Creature receiver) {
            return source != null ? source : receiver;
        }
    }

    /** What some conditions read besides the board: the blow's target and start values, the trigger's context. */
    record Ctx(Creature target, CycleStart start, TriggerContext trigger, Boolean wouldKill) {

        static final Ctx NONE = new Ctx(null, null, TriggerContext.NONE, null);

        static Ctx of(TriggerContext trigger) {
            return new Ctx(null, null, trigger, null);
        }
    }

    private final AbilityBinder binder;
    private final MatchState state;
    private final Evaluation evaluation;
    private final int cycle;
    private final Map<Creature, List<BoundExpansion>> layer2 = new IdentityHashMap<>();
    private List<Active> statics;
    private boolean computingLayers;

    /**
     * @param evaluation the turn's evaluation (hands, attackers), null before the first Evaluate
     * @param cycle      1 or 2 during that combat cycle, 0 otherwise (condition C6)
     */
    Board(AbilityBinder binder, MatchState state, Evaluation evaluation, int cycle) {
        this.binder = binder;
        this.state = state;
        this.evaluation = evaluation;
        this.cycle = cycle;
    }

    MatchState state() {
        return state;
    }

    Evaluation evaluation() {
        return evaluation;
    }

    /** The side {@code c} is on. */
    Side sideOf(Creature c) {
        for (Side side : Side.values()) {
            for (Creature x : state.player(side).creatures()) {
                if (x == c) {
                    return side;
                }
            }
        }
        throw new IllegalStateException("not in play: " + c);
    }

    /** The two sides in resolution order (ruleset 4.3): Potenze Artificiali first; with the same faction, A first. */
    Side[] prioritySides() {
        return TurnResolver.prioritySides(state);
    }

    /** Every creature in play, in lane order, Potenze Artificiali first within a lane, destroyed ones included. */
    @Override
    public List<Creature> everyone() {
        List<Creature> out = new ArrayList<>();
        Side[] order = prioritySides();
        for (int lane = 1; lane <= 5; lane++) {
            for (Side side : order) {
                state.player(side).creatureAt(lane).ifPresent(out::add);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- abilities

    /** Every ability {@code c} has now: its own, plus what static effects grant it, minus what they remove. */
    List<BoundExpansion> abilities(Creature c) {
        List<BoundExpansion> cached = layer2.get(c);
        if (cached != null) {
            return cached;
        }
        List<BoundExpansion> own = c.abilities();
        computingLayers = true;
        List<BoundExpansion> out = new ArrayList<>(own);
        List<String> removed = new ArrayList<>();
        AbilityLayer layer = new AbilityLayer() {
            @Override
            public AbilityBinder binder() {
                return binder;
            }

            @Override
            public boolean has(String abilityId) {
                return out.stream().anyMatch(x -> x.abilityId().equals(abilityId));
            }

            @Override
            public void add(List<BoundExpansion> expansions) {
                out.addAll(expansions);
            }

            @Override
            public void remove(String keywordId) {
                removed.add(keywordId);
            }
        };
        try {
            Side side = sideOf(c);
            for (Active a : layer1Statics()) {
                AbilityLayerEffect effect = EffectDefinitions.find(a.e().effectId())
                        .filter(AbilityLayerEffect.class::isInstance).map(AbilityLayerEffect.class::cast).orElse(null);
                if (effect == null || !reaches(a, side, c) || !holds(a, c, Ctx.NONE)) {
                    continue;
                }
                effect.layer(layer, a.e());
            }
        } finally {
            computingLayers = false;
        }
        if (!removed.isEmpty()) {
            out.removeIf(x -> removed.contains(x.abilityId()));
        }
        List<BoundExpansion> result = List.copyOf(out);
        layer2.put(c, result);
        return result;
    }

    /** The abilities of {@code c} with trigger {@code triggerId}, in order. */
    List<BoundExpansion> abilities(Creature c, String triggerId) {
        return abilities(c).stream().filter(e -> triggerId.equals(e.triggerId())).toList();
    }

    private List<Active> layer1Statics() {
        List<Active> out = new ArrayList<>();
        for (Creature c : everyone()) {
            if (c.destroyed()) {
                continue;
            }
            Side side = sideOf(c);
            for (BoundExpansion e : c.abilities()) {
                if ("T0".equals(e.triggerId())) {
                    out.add(new Active(side, c, e, null));
                }
            }
        }
        out.addAll(auraStatics());
        return out;
    }

    private List<Active> auraStatics() {
        List<Active> out = new ArrayList<>();
        for (Side side : prioritySides()) {
            for (Aura a : state.player(side).auras()) {
                if ("T0".equals(a.expansion().triggerId())) {
                    out.add(new Active(side, null, a.expansion(), a));
                }
            }
        }
        return out;
    }

    /** Every static effect in force: living creatures in lane order (abilities in order), then the spells' auras. */
    List<Active> statics() {
        if (computingLayers) {
            return layer1Statics();
        }
        if (statics == null) {
            List<Active> out = new ArrayList<>();
            for (Creature c : everyone()) {
                if (c.destroyed()) {
                    continue;
                }
                Side side = sideOf(c);
                for (BoundExpansion e : abilities(c)) {
                    if ("T0".equals(e.triggerId())) {
                        out.add(new Active(side, c, e, null));
                    }
                }
            }
            out.addAll(auraStatics());
            statics = out;
        }
        return statics;
    }

    /** The static effects with {@code effectId} that reach {@code receiver} and whose conditions hold. */
    List<Active> reaching(String effectId, Creature receiver, Ctx ctx) {
        if (receiver.destroyed()) {
            return List.of();
        }
        Side side = sideOf(receiver);
        List<Active> out = new ArrayList<>();
        for (Active a : statics()) {
            if (a.e().effectId().equals(effectId) && reaches(a, side, receiver) && holds(a, receiver, ctx)) {
                out.add(a);
            }
        }
        return out;
    }

    private boolean any(String effectId, Creature receiver) {
        return !reaching(effectId, receiver, Ctx.NONE).isEmpty();
    }

    /** The conditions of a static effect: on its source, or on the creature it reaches for a spell's aura. */
    boolean holds(Active a, Creature receiver, Ctx ctx) {
        return holds(a.e(), a.side(), a.sourceOr(receiver), ctx);
    }

    // ---------------------------------------------------------------- reach

    /** Whether the static effect {@code a} reaches {@code receiver}, a creature of {@code side}. */
    boolean reaches(Active a, Side side, Creature receiver) {
        if (receiver.destroyed()) {
            return false;
        }
        Creature source = a.source();
        boolean ok = Targets.find(a.e().targetId())
                .map(t -> t.reaches(this, a.side(), source, side, receiver)).orElse(false);
        return ok && filtersHold(a.e().filters(), a.side(), side, receiver);
    }

    /** {@code target_params.filters}, seen from {@code controller}, on {@code receiver} of {@code side}. */
    @Override
    public boolean filtersHold(List<String> filters, Side controller, Side side, Creature receiver) {
        for (String f : filters) {
            boolean ok = switch (f) {
                case "attacker" -> attacker(side, receiver);
                case "non_attacker" -> !attacker(side, receiver);
                case "own", "ally" -> side == controller;
                case "enemy" -> side != controller;
                case "damaged" -> receiver.currentDef() < receiver.baseDef();
                case "not_damaged" -> receiver.currentDef() >= receiver.baseDef();
                case "damaged_this_turn" -> receiver.damagedThisTurn();
                default -> f.startsWith("suit:") && receiver.card().suit().name()
                        .equals(f.substring(5).toUpperCase(Locale.ROOT));
            };
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    /** True if {@code c} of {@code side} attacks this turn (selected by the hand). */
    @Override
    public boolean attacker(Side side, Creature c) {
        return evaluation != null && evaluation.hand(side).attackerLanes().contains(c.lane());
    }

    /** True if {@code c} is one of the creatures forming the combination of its player's hand (S20). */
    @Override
    public boolean inCombination(Side side, Creature c) {
        if (evaluation == null) {
            return false;
        }
        HandType type = evaluation.hand(side).type();
        return type != HandType.HIGH_CARD && type != HandType.NONE && attacker(side, c);
    }

    @Override
    public List<Creature> creatures(Side side) {
        return state.player(side).creatures();
    }

    @Override
    public Optional<Creature> creatureAt(Side side, int lane) {
        return state.player(side).creatureAt(lane);
    }

    @Override
    public int factionRank(Creature c) {
        return state.player(sideOf(c)).faction() == Faction.POTENZE_ARTIFICIALI ? 0 : 1;
    }

    // ---------------------------------------------------------------- adjacency

    /** The living creatures of {@code side} in the lanes next to {@code c} (S44: lanes differing by 1). */
    @Override
    public List<Creature> neighbours(Side side, Creature c) {
        List<Creature> out = new ArrayList<>();
        for (int lane : new int[] {c.lane() - 1, c.lane() + 1}) {
            if (lane >= 1 && lane <= 5) {
                state.player(side).creatureAt(lane).filter(x -> !x.destroyed()).ifPresent(out::add);
            }
        }
        return out;
    }

    /**
     * Ruleset 7.10.4: two creatures of one player are adjacent when their lanes differ by exactly 1 (lanes 1 and 5
     * are not adjacent), or when an E34 of one of them says the other counts as adjacent to it (its suit, or every
     * creature of its player). A creature reached by E60 is adjacent to no one.
     */
    @Override
    public boolean adjacent(Side side, Creature a, Creature b) {
        return adjacent(side, a, b, true);
    }

    private int adjacencyDepth;

    private boolean adjacent(Side side, Creature a, Creature b, boolean useLinks) {
        if (a == b) {
            return false;
        }
        if (adjacencyDepth == 0 && !computingLayers) {
            adjacencyDepth++;
            try {
                if (isolated(a) || isolated(b)) {
                    return false;
                }
            } finally {
                adjacencyDepth--;
            }
        }
        return Math.abs(a.lane() - b.lane()) == 1
                || (useLinks && (linked(side, a, b) || linked(side, b, a)));
    }

    /** True if {@code x} has a living E34 whose group is the suit of {@code y}, or every creature of its player. */
    private boolean linked(Side side, Creature x, Creature y) {
        if (x.destroyed()) {
            return false;
        }
        List<BoundExpansion> list = computingLayers ? x.abilities() : abilities(x);
        for (BoundExpansion e : list) {
            if (!"T0".equals(e.triggerId()) || !e.effectId().equals(AdjacencyLinkEffect.ID)) {
                continue;
            }
            String group = e.stringParam("group").orElse("");
            boolean matches = group.equals("stesso_giocatore")
                    || group.toUpperCase(Locale.ROOT).equals(y.card().suit().name());
            if (matches && conditionsHold(e, side, x, false, Ctx.NONE)) {
                return true;
            }
        }
        return false;
    }

    /** The living creatures of {@code side} adjacent to {@code c}, in lane order. */
    List<Creature> adjacentAllies(Side side, Creature c) {
        List<Creature> out = new ArrayList<>();
        for (Creature x : state.player(side).creatures()) {
            if (!x.destroyed() && adjacent(side, c, x)) {
                out.add(x);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- what static effects change

    /** E60: adjacent to no creature of its player. */
    boolean isolated(Creature c) {
        return any(IsolatedEffect.ID, c);
    }

    /** E61: positive ATK and DEF modifiers do not apply. */
    boolean noBonus(Creature c) {
        return any(NoBonusEffect.ID, c);
    }

    /** E52: its ATK cannot be lowered by effects. */
    boolean noReduce(Creature c) {
        return any(NoAtkReductionEffect.ID, c);
    }

    /** E18: its Evolutions cannot fire. */
    boolean evolutionsSuppressed(Creature c) {
        return any(SuppressEvolutionEffect.ID, c);
    }

    /** E26: any combat damage it deals destroys. */
    boolean lethal(Creature c) {
        return any(LethalDamageEffect.ID, c);
    }

    /** E55: counts as any suit for the hand. */
    boolean wildSuit(Creature c) {
        return any(WildSuitEffect.ID, c);
    }

    /** E54: the largest rank step it may use for straights, 0 if none. */
    int rankStep(Creature c) {
        int step = 0;
        for (Active a : reaching(RankStepEffect.ID, c, Ctx.NONE)) {
            step = Math.max(step, a.e().intParam("step", 1));
        }
        return step;
    }

    /** The ATK of {@code c} with the modifiers effects left on it, protections (E52, E61) applied. */
    int ownAtk(Creature c) {
        return c.atk(noBonus(c), noReduce(c));
    }

    /**
     * The ATK static E1 effects add to {@code receiver} (one modifier per effect, sources in lane order and each
     * source's abilities in order, then the spells), with E52 and E61 applied. {@code target} and {@code start} are
     * the blow's, for C22; null when there is no blow.
     */
    List<Modifier> atkBonuses(Creature receiver, Creature target, CycleStart start) {
        List<Modifier> out = new ArrayList<>();
        if (receiver.destroyed()) {
            return out;
        }
        Side side = sideOf(receiver);
        boolean noBonus = noBonus(receiver);
        boolean noReduce = noReduce(receiver);
        Ctx ctx = new Ctx(target, start, TriggerContext.NONE, null);
        for (Active a : statics()) {
            if (!a.e().effectId().equals(AtkEffect.ID) || !reaches(a, side, receiver) || !holds(a, receiver, ctx)) {
                continue;
            }
            int amount = AtkEffect.amount(a.e(), receiver.card().atk(), AtkEffect.scale(this, side, a.e()));
            if (amount == 0 || (amount > 0 && noBonus) || (amount < 0 && noReduce)) {
                continue;
            }
            Creature from = a.sourceOr(receiver);
            out.add(new Modifier(CreatureRef.of(sideOf(from), from), a.e().abilityId(), AtkEffect.ID,
                    amount > 0 ? Modifier.Kind.ATK_BONUS : Modifier.Kind.ATK_MALUS, Math.abs(amount)));
        }
        return out;
    }

    /**
     * The damage reductions (static E28) that reach {@code target}: in the order of {@link #statics()}, so a
     * creature's own reductions come in card order.
     */
    List<Active> reductions(Creature target) {
        return reaching(DamageReductionEffect.ID, target, Ctx.NONE);
    }

    /** The share (per mille) of a heal {@code c} receives: E51 replacements multiplied, 1000 with none. */
    int healPermille(Creature c) {
        long factor = 1000;
        for (Active a : replacements("T23", c)) {
            if (a.e().effectId().equals(HealFactorEffect.ID)) {
                factor = factor * a.e().intParam("permille") / 1000;
            }
        }
        return Math.toIntExact(factor);
    }

    /**
     * The replacement (class R) effects with {@code triggerId} that apply to {@code c}: its own abilities (target S1),
     * then the auras of spells that reach it, conditions holding and triggers left.
     */
    List<Active> replacements(String triggerId, Creature c) {
        List<Active> out = new ArrayList<>();
        Side side = sideOf(c);
        for (BoundExpansion e : abilities(c, triggerId)) {
            if (c.canTrigger(e, state.turn()) && holds(e, side, c, Ctx.NONE)) {
                out.add(new Active(side, c, e, null));
            }
        }
        for (Side owner : prioritySides()) {
            for (Aura aura : state.player(owner).auras()) {
                BoundExpansion e = aura.expansion();
                if (!triggerId.equals(e.triggerId()) || !aura.canTrigger()) {
                    continue;
                }
                Active a = new Active(owner, null, e, aura);
                if (reachesAura(a, side, c) && holds(e, owner, c, Ctx.NONE)) {
                    out.add(a);
                }
            }
        }
        return out;
    }

    private boolean reachesAura(Active a, Side side, Creature c) {
        return switch (a.e().targetId()) {
            case TargetIds.HIGHEST_RANK -> select(TargetIds.HIGHEST_RANK, a.e().targetParams(), a.side()).filter(x -> x == c).isPresent();
            default -> reaches(a, side, c);
        };
    }

    // ---------------------------------------------------------------- conditions

    /** True if the conditions of {@code e}, an ability of {@code source} (a creature of {@code side}), hold now. */
    boolean holds(BoundExpansion e, Side side, Creature source, Ctx ctx) {
        return conditionsHold(e, side, source, true, ctx);
    }

    private boolean conditionsHold(BoundExpansion e, Side side, Creature source, boolean useLinks, Ctx ctx) {
        ConditionContext view = new Eval(useLinks, ctx);
        for (ConditionDef c : e.conditions()) {
            Condition condition = Conditions.find(c.id()).orElseThrow(() -> new IllegalStateException(
                    "condition " + c.id() + " has no evaluation (EffectRegistry let it through)"));
            if (!condition.holds(view, c, side, source)) {
                return false;
            }
        }
        return true;
    }

    /** What the conditions of one evaluation may read: this board, the blow or trigger in {@code ctx}. */
    private final class Eval implements ConditionContext {

        private final boolean useLinks;
        private final Ctx ctx;

        Eval(boolean useLinks, Ctx ctx) {
            this.useLinks = useLinks;
            this.ctx = ctx;
        }

        @Override
        public boolean attacker(Side side, Creature creature) {
            return Board.this.attacker(side, creature);
        }

        @Override
        public int suitCount(Side side, Suit suit) {
            return state.player(side).suitCount(suit);
        }

        @Override
        public boolean firstCycle() {
            return cycle == 1;
        }

        @Override
        public Optional<HandType> handType(Side side) {
            return evaluation == null ? Optional.empty() : Optional.of(evaluation.hand(side).type());
        }

        @Override
        public boolean hasAdjacentOfSuit(Side side, Creature source, Suit suit) {
            return state.player(side).creatures().stream().anyMatch(o -> o != source && !o.destroyed()
                    && o.card().suit() == suit && adjacent(side, source, o, useLinks));
        }

        @Override
        public boolean wouldKill() {
            return Boolean.TRUE.equals(ctx.wouldKill());
        }

        @Override
        public boolean targetDefAboveAtkAtCycleStart(Creature source) {
            return ctx.target() != null && ctx.start() != null && ctx.start().def(ctx.target()) > ctx.start().atk(source);
        }

        @Override
        public boolean locked(Side side, String key) {
            return state.player(side).isLocked(key);
        }

        @Override
        public boolean vacatedSuit(Side side, int lane, Suit suit) {
            return state.player(side).vacatedSuit(lane).filter(s -> s == suit).isPresent();
        }

        @Override
        public int adjacentAllyCount(Side side, Creature creature) {
            return adjacentAllies(side, creature).size();
        }

        @Override
        public TriggerContext trigger() {
            return ctx.trigger();
        }

        @Override
        public boolean wasAdjacent(Side side, Creature source, Creature destroyed, int lane) {
            return Board.this.wasAdjacent(side, source, destroyed, lane);
        }
    }

    /** C28: the destroyed creature was in a lane next to the source (or linked to it by E34) when it was destroyed. */
    private boolean wasAdjacent(Side side, Creature source, Creature destroyed, int lane) {
        return Math.abs(lane - source.lane()) == 1 || linked(side, source, destroyed);
    }

    // ---------------------------------------------------------------- selection (target_selection_rules)

    /**
     * A selection target ({@code S33} lowest lane, {@code S35} highest ATK, {@code S37} highest DEF, {@code S38}
     * lowest DEF, {@code S39} highest rank, {@code S40} lowest rank) among the living creatures of both sides that
     * pass the filters (seen from {@code controller}; {@code from: S10} is the opponent's creatures). Ties as in each
     * target's description: lowest lane, Potenze Artificiali first, then rank, then suit order.
     */
    @Override
    public Optional<Creature> select(String targetId, Map<String, Value> params, Side controller) {
        RankedTarget ranked = Targets.find(targetId).filter(RankedTarget.class::isInstance).map(RankedTarget.class::cast)
                .orElseThrow(() -> new IllegalStateException("not a selection target: " + targetId));
        List<String> filters = new ArrayList<>();
        if (params.get("filters") instanceof Value.ListV l) {
            l.items().forEach(v -> filters.add(((Value.StrV) v).v()));
        }
        if (params.get("from") instanceof Value.StrV from) {
            switch (from.v()) {
                case TargetIds.OWN_CREATURES -> filters.add("own");
                case TargetIds.ENEMY_CREATURES -> filters.add("enemy");
                default -> { }
            }
        }
        List<Creature> pool = new ArrayList<>();
        for (Creature c : everyone()) {
            if (!c.destroyed() && filtersHold(filters, controller, sideOf(c), c)) {
                pool.add(c);
            }
        }
        return pool.stream().min(ranked.order(this));
    }
}
