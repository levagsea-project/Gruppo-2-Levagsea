package quorma.gameengine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.Value;
import quorma.gameengine.hand.HandResult;
import quorma.gameengine.input.MatchInput;
import quorma.gameengine.json.CanonicalJson;
import quorma.gameengine.log.BoardSnapshot;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.log.MatchEvent.AttackSkipped;
import quorma.gameengine.log.MatchEvent.CardDrawn;
import quorma.gameengine.log.MatchEvent.CardSetAside;
import quorma.gameengine.log.MatchEvent.CreatureDestroyed;
import quorma.gameengine.log.MatchEvent.CreatureDiscarded;
import quorma.gameengine.log.MatchEvent.DeckReshuffled;
import quorma.gameengine.log.MatchEvent.EffectFizzled;
import quorma.gameengine.log.MatchEvent.EffectResolved;
import quorma.gameengine.log.MatchEvent.EffectTriggered;
import quorma.gameengine.log.MatchEvent.EquipmentApplied;
import quorma.gameengine.log.MatchEvent.EquipmentLost;
import quorma.gameengine.log.MatchEvent.EquipmentPending;
import quorma.gameengine.log.MatchEvent.LaneLeftEmpty;
import quorma.gameengine.log.MatchEvent.SpellResolved;
import quorma.gameengine.log.Modifier;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Ability;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.Attack;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.MatchResult;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.SpellCard;
import quorma.gameengine.turn.Evaluation;
import quorma.gameengine.turn.TurnReport;

/**
 * Writes a {@link Simulation} as the canonical MatchLog ({@code schemas/match-log.schema.json}, decision
 * {@code 2026-11-14-match-log-schema}): keys in schema order, {@link CanonicalJson} text, {@code result_hash} =
 * SHA-256 of the canonical bytes of the log without that key, then added as the last key.
 *
 * <p>Events name creatures by side and lane; the card ids of attackers and targets come from the board at the start
 * of the turn (the board after setup, or the previous turn's end board): nobody enters play during combat.</p>
 */
final class MatchLogWriter {

    private MatchLogWriter() {}

    static String write(Simulation sim) {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("schema_version", 1);
        log.put("header", header(sim));

        Map<String, Object> setup = new LinkedHashMap<>();
        setup.put("events", events(sim.setupEvents(), sim.setupBoard()));
        setup.put("board", board(sim.setupBoard()));
        log.put("setup", setup);

        List<Object> turns = new ArrayList<>();
        BoardSnapshot start = sim.setupBoard();
        for (TurnReport turn : sim.report().turns()) {
            turns.add(turn(turn, start));
            start = turn.board();
        }
        log.put("turns", turns);
        log.put("result", result(sim));
        log.put("totals", totals(sim));
        log.put("result_hash", CanonicalJson.sha256(log));
        return CanonicalJson.write(log);
    }

    // ---- header ----

    private static Map<String, Object> header(Simulation sim) {
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("engine_version", sim.engineVersion());
        h.put("ruleset_version", sim.rulesetVersion());
        h.put("ruleset_hash", sim.rulesetHash());
        h.put("prng", "SplitMix64");
        h.put("shuffle", "FISHER_YATES");
        h.put("seed", sim.input().seed());
        h.put("starting_life", sim.startingLife());
        Map<String, Object> players = new LinkedHashMap<>();
        for (Side side : Side.values()) {
            MatchInput.Player p = sim.input().player(side);
            Map<String, Object> player = new LinkedHashMap<>();
            player.put("faction", p.faction().name());
            p.poolSnapshotId().ifPresent(id -> player.put("pool_snapshot_id", id));
            player.put("deck", p.deck().stream().map(MatchLogWriter::card).toList());
            player.put("draw_order", sim.drawOrder().get(side));
            players.put(side.name(), player);
        }
        h.put("players", players);
        return h;
    }

    /** A card as in the match input: keys in schema order, empty params, conditions and trigger params left out. */
    private static Map<String, Object> card(Card card) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("id", card.id());
        switch (card) {
            case CreatureCard cc -> {
                c.put("kind", "CREATURE");
                c.put("rank", cc.rank());
                c.put("suit", cc.suit().name());
                c.put("subfaction", cc.subfaction().name());
                c.put("atk", cc.atk());
                c.put("def", cc.def());
            }
            case SpellCard s -> {
                c.put("kind", "SPELL");
                c.put("faction", s.faction().name());
            }
            case EquipmentCard e -> {
                c.put("kind", "EQUIPMENT");
                c.put("faction", e.faction().name());
            }
        }
        c.put("abilities", card.abilities().stream().map(MatchLogWriter::ability).toList());
        return c;
    }

    private static Map<String, Object> ability(Ability ability) {
        Map<String, Object> a = new LinkedHashMap<>();
        switch (ability) {
            case AbilityRef ref -> {
                a.put("id", ref.id());
                if (!ref.params().isEmpty()) {
                    a.put("params", values(ref.params()));
                }
            }
            case InlineEffect inline -> a.put("expansions", inline.expansions().stream().map(MatchLogWriter::expansion).toList());
        }
        return a;
    }

    private static Map<String, Object> expansion(ExpansionDef x) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("trigger_id", x.triggerId());
        putIfAny(e, "trigger_params", x.triggerParams());
        e.put("effect_id", x.effectId());
        putIfAny(e, "effect_params", x.effectParams());
        e.put("target_id", x.targetId());
        putIfAny(e, "target_params", x.targetParams());
        e.put("duration", x.duration());
        if (!x.conditions().isEmpty()) {
            List<Object> conditions = new ArrayList<>();
            for (ConditionDef cond : x.conditions()) {
                Map<String, Object> c = new LinkedHashMap<>();
                c.put("id", cond.id());
                putIfAny(c, "params", cond.params());
                conditions.add(c);
            }
            e.put("conditions", conditions);
        }
        e.put("class", x.cls().name());
        x.common().forEach((k, v) -> e.put(k, Value.toJson(v)));
        return e;
    }

    private static void putIfAny(Map<String, Object> out, String key, Map<String, Value> params) {
        if (!params.isEmpty()) {
            out.put(key, values(params));
        }
    }

    private static Map<String, Object> values(Map<String, Value> params) {
        Map<String, Object> out = new LinkedHashMap<>();
        params.forEach((k, v) -> out.put(k, Value.toJson(v)));
        return out;
    }

    // ---- turns ----

    private static Map<String, Object> turn(TurnReport t, BoardSnapshot start) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("turn", t.turn());
        Map<String, Object> evaluation = new LinkedHashMap<>();
        for (Side side : Side.values()) {
            evaluation.put(side.name(), hand(t.evaluation(), side));
        }
        out.put("evaluation", evaluation);
        List<Object> declarations = new ArrayList<>();
        for (Attack attack : t.attacks()) {
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("attacker", ref(start, attack.attacker(), attack.attackerLane()));
            d.put("target", attack.target().map(target -> (Object) ref(start, target.side(), target.lane())).orElse(null));
            d.put("cycle", attack.cycle());
            declarations.add(d);
        }
        out.put("declarations", declarations);
        out.put("cycle_1", events(t.cycle1(), start));
        out.put("cycle_2", events(t.cycle2(), start));
        out.put("refill", events(t.refill(), start));
        out.put("board", board(t.board()));
        return out;
    }

    private static Map<String, Object> hand(Evaluation evaluation, Side side) {
        HandResult hand = evaluation.hand(side);
        Map<String, Object> h = new LinkedHashMap<>();
        h.put("hand_type", hand.type().name());
        h.put("attacker_lanes", hand.attackerLanes());
        h.put("tiebreak", hand.tiebreak());
        h.put("multiplier_permille", evaluation.multiplier(side));
        return h;
    }

    private static List<Object> events(List<MatchEvent> events, BoardSnapshot start) {
        List<Object> out = new ArrayList<>();
        for (MatchEvent e : events) {
            out.add(event(e, start));
        }
        return out;
    }

    private static Map<String, Object> event(MatchEvent event, BoardSnapshot start) {
        Map<String, Object> e = new LinkedHashMap<>();
        switch (event) {
            case AttackResolved a -> {
                e.put("type", "attack_resolved");
                e.put("cycle", a.cycle());
                e.put("attacker", ref(start, a.attack().attacker(), a.attack().attackerLane()));
                e.put("target", ref(start, a.attack().target().orElseThrow().side(), a.attack().target().orElseThrow().lane()));
                e.put("atk", a.atk());
                e.put("advantage_permille", a.advantagePermille());
                e.put("effective_atk", a.effectiveAtk());
                e.put("mitigation_permille", a.mitigationPermille());
                e.put("damage", a.damage());
                e.put("def_before", a.defBefore());
                e.put("def_after", a.defAfter());
                e.put("killed", a.killed());
                e.put("mutual_kill", a.mutualKill());
                e.put("hand_multiplier_permille", a.handMultiplierPermille());
                e.put("spill_multiplier_permille", a.spillMultiplierPermille());
                e.put("spill", a.spill());
                e.put("modifiers", a.modifiers().stream().map(MatchLogWriter::modifier).toList());
            }
            case AttackSkipped s -> {
                e.put("type", "attack_skipped");
                e.put("cycle", s.cycle());
                e.put("attacker", ref(start, s.attack().attacker(), s.attack().attackerLane()));
                e.put("reason", s.reason().name());
            }
            case CreatureDestroyed d -> {
                e.put("type", "creature_destroyed");
                e.put("creature", ref(d.side(), d.lane(), d.cardId()));
            }
            case EffectTriggered t -> {
                e.put("type", "effect_triggered");
                e.put("source", ref(t.source()));
                e.put("ability_id", t.abilityId());
                e.put("trigger_id", t.triggerId());
                e.put("effect_id", t.effectId());
                e.put("stack_size", t.stackSize());
            }
            case EffectResolved r -> {
                e.put("type", "effect_resolved");
                e.put("source", ref(r.source()));
                e.put("ability_id", r.abilityId());
                e.put("effect_id", r.effectId());
                e.put("target", ref(r.target()));
                e.put("changes", r.changes().stream().map(MatchLogWriter::change).toList());
                r.stacks().ifPresent(n -> e.put("stacks", n));
            }
            case EffectFizzled f -> {
                e.put("type", "effect_fizzled");
                e.put("source", ref(f.source()));
                e.put("ability_id", f.abilityId());
                e.put("effect_id", f.effectId());
                e.put("reason", f.reason().name());
            }
            case CreatureDiscarded d -> {
                e.put("type", "creature_discarded");
                e.put("creature", ref(d.side(), d.lane(), d.cardId()));
            }
            case CardDrawn d -> {
                e.put("type", "card_drawn");
                e.put("side", d.side().name());
                e.put("lane", d.lane());
                e.put("card_id", d.cardId());
            }
            case CardSetAside a -> {
                e.put("type", "card_set_aside");
                e.put("side", a.side().name());
                e.put("card_id", a.cardId());
            }
            case DeckReshuffled r -> {
                e.put("type", "deck_reshuffled");
                e.put("side", r.side().name());
                e.put("cards_returned", r.cardsReturned());
                e.put("draw_order", r.drawOrder());
            }
            case SpellResolved s -> {
                e.put("type", "spell_resolved");
                e.put("side", s.side().name());
                e.put("lane", s.lane());
                e.put("card_id", s.cardId());
            }
            case EquipmentPending p -> {
                e.put("type", "equipment_pending");
                e.put("side", p.side().name());
                e.put("card_id", p.cardId());
            }
            case EquipmentApplied a -> {
                e.put("type", "equipment_applied");
                e.put("side", a.side().name());
                e.put("lane", a.lane());
                e.put("equipment_id", a.equipmentId());
                e.put("creature_id", a.creatureId());
            }
            case EquipmentLost l -> {
                e.put("type", "equipment_lost");
                e.put("side", l.side().name());
                e.put("card_id", l.cardId());
            }
            case LaneLeftEmpty l -> {
                e.put("type", "lane_left_empty");
                e.put("side", l.side().name());
                e.put("lane", l.lane());
            }
            case MatchEvent.HandEvaluated h -> throw notAPhaseEvent(h);
            case MatchEvent.TargetDeclared d -> throw notAPhaseEvent(d);
            case MatchEvent.MatchEnded m -> throw notAPhaseEvent(m);
        }
        return e;
    }

    private static IllegalStateException notAPhaseEvent(MatchEvent e) {
        return new IllegalStateException("not an event of a phase list, it has its own place in the log: " + e);
    }

    private static Map<String, Object> modifier(Modifier m) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("source", ref(m.source()));
        out.put("ability_id", m.abilityId());
        out.put("effect_id", m.effectId());
        out.put("kind", m.kind().name().toLowerCase(Locale.ROOT));
        out.put("value", m.value());
        return out;
    }

    private static Map<String, Object> change(StatChange c) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("stat", c.stat().name().toLowerCase(Locale.ROOT));
        out.put("before", c.before());
        out.put("after", c.after());
        return out;
    }

    private static Map<String, Object> ref(CreatureRef r) {
        return ref(r.side(), r.lane(), r.cardId());
    }

    private static Map<String, Object> ref(Side side, int lane, String cardId) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("side", side.name());
        out.put("lane", lane);
        out.put("card_id", cardId);
        return out;
    }

    /** The creature in {@code lane} of {@code side} on the board at the start of the turn. */
    private static Map<String, Object> ref(BoardSnapshot start, Side side, int lane) {
        String cardId = start.side(side).cardId(lane).orElseThrow(() -> new IllegalStateException(
                "no creature in lane " + lane + " of " + side + " at the start of the turn"));
        return ref(side, lane, cardId);
    }

    // ---- board, result, totals ----

    private static Map<String, Object> board(BoardSnapshot board) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Side side : Side.values()) {
            BoardSnapshot.PlayerBoard p = board.side(side);
            Map<String, Object> player = new LinkedHashMap<>();
            player.put("life", p.life());
            player.put("deck_count", p.deckCount());
            player.put("discard_count", p.discardCount());
            List<Object> lanes = new ArrayList<>();
            for (Optional<BoardSnapshot.LaneState> lane : p.lanes()) {
                lanes.add(lane.map(MatchLogWriter::lane).orElse(null));
            }
            player.put("lanes", lanes);
            out.put(side.name(), player);
        }
        return out;
    }

    private static Object lane(BoardSnapshot.LaneState l) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("card_id", l.cardId());
        out.put("atk", l.atk());
        out.put("def", l.def());
        out.put("destroyed", l.destroyed());
        out.put("stacks", new LinkedHashMap<String, Object>(l.stacks()));
        return out;
    }

    private static Map<String, Object> result(Simulation sim) {
        List<TurnReport> turns = sim.report().turns();
        MatchResult result = sim.report().result();
        BoardSnapshot end = turns.get(turns.size() - 1).board();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("outcome", result.outcome().name());
        out.put("reason", result.reason().name());
        out.put("turns_played", turns.size());
        Map<String, Object> life = new LinkedHashMap<>();
        for (Side side : Side.values()) {
            life.put(side.name(), end.side(side).life());
        }
        out.put("life", life);
        return out;
    }

    /** Sums of the events (schema {@code totals}): attacks by the attacking card, losses and draws by owner. */
    private static Map<String, Object> totals(Simulation sim) {
        // Looked up by key only, never iterated: output order comes from the decks and Side.values().
        Map<Key, long[]> cards = new HashMap<>(); // times_drawn, damage_dealt, spill_damage, kills
        Map<Side, long[]> players = new HashMap<>(); // damage_dealt, spill_damage, kills, creatures_lost
        for (Side side : Side.values()) {
            players.put(side, new long[4]);
            for (Card card : sim.input().player(side).deck()) {
                cards.put(new Key(side, card.id()), new long[4]);
            }
        }
        countDraws(sim.setupEvents(), cards);
        BoardSnapshot start = sim.setupBoard();
        for (TurnReport turn : sim.report().turns()) {
            for (MatchEvent event : turn.events()) {
                if (event instanceof AttackResolved a) {
                    Side side = a.attack().attacker();
                    String cardId = start.side(side).cardId(a.attack().attackerLane()).orElseThrow();
                    long[] c = cards.get(new Key(side, cardId));
                    long[] p = players.get(side);
                    c[1] += a.damage();
                    c[2] += a.spill();
                    p[0] += a.damage();
                    p[1] += a.spill();
                    if (a.killed()) {
                        c[3]++;
                        p[2]++;
                    }
                } else if (event instanceof CreatureDestroyed d) {
                    players.get(d.side())[3]++;
                }
            }
            countDraws(turn.refill(), cards);
            start = turn.board();
        }

        Map<String, Object> out = new LinkedHashMap<>();
        Map<String, Object> playerTotals = new LinkedHashMap<>();
        for (Side side : Side.values()) {
            long[] p = players.get(side);
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("damage_dealt", p[0]);
            t.put("spill_damage", p[1]);
            t.put("kills", p[2]);
            t.put("creatures_lost", p[3]);
            playerTotals.put(side.name(), t);
        }
        out.put("players", playerTotals);
        List<Object> cardTotals = new ArrayList<>();
        for (Side side : Side.values()) {
            for (Card card : sim.input().player(side).deck()) {
                long[] c = cards.get(new Key(side, card.id()));
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("side", side.name());
                t.put("card_id", card.id());
                t.put("times_drawn", c[0]);
                t.put("damage_dealt", c[1]);
                t.put("spill_damage", c[2]);
                t.put("kills", c[3]);
                cardTotals.add(t);
            }
        }
        out.put("cards", cardTotals);
        return out;
    }

    /** A card of a deck: side and id (ids are unique within a deck). */
    private record Key(Side side, String cardId) {}

    private static void countDraws(List<MatchEvent> events, Map<Key, long[]> cards) {
        for (MatchEvent event : events) {
            if (event instanceof CardDrawn d) {
                cards.get(new Key(d.side(), d.cardId()))[0]++;
            }
        }
    }
}
