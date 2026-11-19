package quorma.gameengine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.input.MatchInput;
import quorma.gameengine.input.MatchInputReader;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.Side;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The whole base set ({@code docs/cards/card-effects.json}, exported to {@code data/sets/BASE.json}
 * by {@code tools/base-set/export_cards.py}): every card binds, and matches between decks drawn from the set play to
 * the end with a log that fits its schema, the same bytes for the same input. Prints which effects and triggers the
 * matches used.
 */
class BaseSetTest {

    private static final GameEngine ENGINE = new GameEngine();
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Schema LOG = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_2020_12)
            .getSchema(SchemaLocation.of("classpath:schemas/match-log.schema.json"));
    private static final int MATCHES = 300;

    private static final List<JsonNode> RES_CREATURES = new ArrayList<>();
    private static final List<JsonNode> RES_CARDS = new ArrayList<>();
    private static final List<JsonNode> AI_CREATURES = new ArrayList<>();
    private static final List<JsonNode> AI_CARDS = new ArrayList<>();

    @BeforeAll
    static void load() throws IOException {
        JsonNode root = MAPPER.readTree(Files.readString(Path.of("..", "data", "sets", "BASE.json"),
                StandardCharsets.UTF_8));
        for (JsonNode card : root.get("cards")) {
            boolean res = card.get("id").asString().startsWith("BASE-RES-");
            boolean creature = card.get("kind").asString().equals("CREATURE");
            (res ? (creature ? RES_CREATURES : RES_CARDS) : (creature ? AI_CREATURES : AI_CARDS)).add(card);
        }
    }

    @Test
    void theSetIsComplete() {
        assertEquals(52, RES_CREATURES.size());
        assertEquals(52, AI_CREATURES.size());
        assertEquals(23, RES_CARDS.size());
        assertEquals(23, AI_CARDS.size());
    }

    @Test
    void everyCardBinds() {
        AbilityBinder binder = new AbilityBinder(ENGINE.rules());
        for (List<JsonNode> group : List.of(RES_CREATURES, RES_CARDS, AI_CREATURES, AI_CARDS)) {
            for (int from = 0; from < group.size(); from += 32) {
                List<JsonNode> slice = group.subList(from, Math.min(group.size(), from + 32));
                boolean ai = group == AI_CREATURES || group == AI_CARDS;
                MatchInput input = MatchInputReader.read(input(1, slice, (ai ? RES_CREATURES : AI_CREATURES).subList(0, 5), ai));
                for (Side side : Side.values()) {
                    for (Card card : input.player(side).deck()) {
                        if (card instanceof CreatureCard c) {
                            binder.bind(c);
                        } else {
                            binder.bindImmediate(card);
                            binder.bindLasting(card);
                        }
                    }
                }
            }
        }
    }

    @Test
    void decksFromTheSetPlayToTheEnd() {
        TreeMap<String, Integer> effects = new TreeMap<>();
        TreeMap<String, Integer> triggers = new TreeMap<>();
        TreeMap<String, Integer> events = new TreeMap<>();
        TreeMap<String, Integer> outcomes = new TreeMap<>();
        int turns = 0;
        for (int seed = 0; seed < MATCHES; seed++) {
            Random random = new Random(seed);
            List<JsonNode> res = deck(random, RES_CREATURES, RES_CARDS);
            List<JsonNode> ai = deck(random, AI_CREATURES, AI_CARDS);
            String input = seed % 2 == 0 ? input(seed, res, ai, false) : input(seed, ai, res, true);
            String log = ENGINE.matchLog(input);
            if (seed < 40) {
                List<String> errors = LOG.validate(MAPPER.readTree(log)).stream().map(Error::toString).toList();
                assertEquals(List.of(), errors, "seed " + seed + ": the log fits its schema");
            }
            if (seed < 10) {
                assertEquals(log, ENGINE.matchLog(input), "seed " + seed + ": same input, same bytes");
            }
            JsonNode tree = MAPPER.readTree(log);
            outcomes.merge(tree.get("result").get("outcome").asString() + " " + tree.get("result").get("reason").asString(),
                    1, Integer::sum);
            for (JsonNode turn : tree.get("turns")) {
                turns++;
                for (String phase : List.of("opening", "cycle_1", "cycle_2", "refill", "end_of_turn")) {
                    if (turn.get(phase) == null) {
                        continue;
                    }
                    for (JsonNode e : turn.get(phase)) {
                        String type = e.get("type").asString();
                        events.merge(type, 1, Integer::sum);
                        if (e.get("effect_id") != null && !type.equals("effect_triggered")) {
                            effects.merge(e.get("effect_id").asString(), 1, Integer::sum);
                        }
                        if (e.get("trigger_id") != null) {
                            triggers.merge(e.get("trigger_id").asString(), 1, Integer::sum);
                        }
                        if (e.get("modifiers") != null) {
                            for (JsonNode m : e.get("modifiers")) {
                                effects.merge(m.get("effect_id").asString(), 1, Integer::sum);
                            }
                        }
                    }
                }
            }
        }
        System.out.println("base set, " + MATCHES + " matches, " + turns + " turns");
        System.out.println("  outcomes: " + outcomes);
        System.out.println("  effects:  " + effects);
        System.out.println("  triggers: " + triggers);
        System.out.println("  events:   " + events);
        assertTrue(turns >= MATCHES);
        Set<String> used = usedEffects();
        Set<String> missing = new TreeSet<>(used);
        missing.removeAll(effects.keySet());
        System.out.println("  effects of the set with no log event carrying their effect_id (passive ones: static, hand and"
                + " replacement effects, whose use shows in creature_saved, attack_skipped, refill_prevented or in the numbers"
                + " themselves; not a list of effects that never ran): " + missing);
    }

    /** Every effect id the set's cards use, from the cards as the engine reads them. */
    private static Set<String> usedEffects() {
        Set<String> out = new TreeSet<>();
        List<JsonNode> res = new ArrayList<>(RES_CREATURES);
        res.addAll(RES_CARDS);
        List<JsonNode> ai = new ArrayList<>(AI_CREATURES);
        ai.addAll(AI_CARDS);
        for (MatchInput input : List.of(
                MatchInputReader.read(input(1, res.subList(0, 64), ai.subList(0, 64), false)),
                MatchInputReader.read(input(1, res.subList(11, 75), ai.subList(11, 75), false)))) {
        for (Side side : Side.values()) {
            for (Card card : input.player(side).deck()) {
                for (var a : card.abilities()) {
                    if (a instanceof InlineEffect inline) {
                        for (ExpansionDef x : inline.expansions()) {
                            out.add(x.effectId());
                        }
                    }
                }
            }
        }
        }
        return out;
    }

    /** A deck of 24 creatures and 8 spells or equipment, drawn from the set. */
    private static List<JsonNode> deck(Random random, List<JsonNode> creatures, List<JsonNode> cards) {
        List<JsonNode> c = new ArrayList<>(creatures);
        List<JsonNode> e = new ArrayList<>(cards);
        Collections.shuffle(c, random);
        Collections.shuffle(e, random);
        List<JsonNode> deck = new ArrayList<>(c.subList(0, 24));
        deck.addAll(e.subList(0, 8));
        return deck;
    }

    private static String input(long seed, List<JsonNode> deckA, List<JsonNode> deckB, boolean aIsPotenze) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("schema_version", 1);
        root.put("seed", seed);
        root.put("ruleset_version", "1.0-alpha");
        ObjectNode players = root.putObject("players");
        player(players.putObject("A"), aIsPotenze ? "POTENZE_ARTIFICIALI" : "RESISTENZA_UMANA", deckA);
        player(players.putObject("B"), aIsPotenze ? "RESISTENZA_UMANA" : "POTENZE_ARTIFICIALI", deckB);
        return MAPPER.writeValueAsString(root);
    }

    private static void player(ObjectNode player, String faction, List<JsonNode> deck) {
        player.put("faction", faction);
        ArrayNode cards = player.putArray("deck");
        for (JsonNode card : deck) {
            ObjectNode engineCard = (ObjectNode) card.deepCopy();
            engineCard.remove("sets"); // catalogue data, not known to the engine schema
            cards.add(engineCard);
        }
    }
}
