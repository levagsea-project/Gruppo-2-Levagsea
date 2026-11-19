package quorma.gameengine.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import quorma.gameengine.EngineVersion;
import quorma.gameengine.GameEngine;
import quorma.gameengine.cards.CardCheckReport.CardError;
import quorma.gameengine.cards.CardCheckReport.PoolError;
import quorma.gameengine.input.MatchInputException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** The card pool check: the whole base set passes, and each rule rejects what it should, with its own code. */
class CardPoolCheckerTest {

    private static final GameEngine ENGINE = new GameEngine();
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final List<String> RES = new ArrayList<>();
    private static final List<String> AI = new ArrayList<>();
    private static final List<String> RES_IDS = new ArrayList<>();
    private static final List<String> AI_IDS = new ArrayList<>();

    @BeforeAll
    static void load() throws IOException {
        JsonNode root = MAPPER.readTree(Files.readString(Path.of("..", "data", "sets", "BASE.json"), StandardCharsets.UTF_8));
        for (JsonNode card : root.get("cards")) {
            ObjectNode engineCard = (ObjectNode) card.deepCopy();
            engineCard.remove("sets"); // catalogue data, not known to the engine
            boolean res = card.get("id").asString().startsWith("BASE-RES-");
            (res ? RES : AI).add(MAPPER.writeValueAsString(engineCard));
            (res ? RES_IDS : AI_IDS).add(card.get("id").asString());
        }
    }

    // ---- helpers ----

    private static String request(String faction, List<String> cards, String pools) {
        return "{\"schema_version\":1,\"faction\":\"" + faction + "\",\"cards\":[" + String.join(",", cards) + "]"
                + (pools == null ? "" : ",\"pools\":[" + pools + "]") + "}";
    }

    private static String res(List<String> cards) {
        return request("RESISTENZA_UMANA", cards, null);
    }

    /** A Resistenza Umana creature (Adattati, Vincolo). */
    private static String creature(String id, int rank, String abilities) {
        return "{\"id\":\"" + id + "\",\"kind\":\"CREATURE\",\"rank\":" + rank + ",\"suit\":\"VINCOLO\","
                + "\"subfaction\":\"ADATTATI\",\"atk\":3000,\"def\":5000,\"abilities\":[" + abilities + "]}";
    }

    private static CardCheckReport check(String request) {
        return ENGINE.checkCards(request);
    }

    private static List<String> cardCodes(CardCheckReport r) {
        return r.cardErrors().stream().map(CardError::code).collect(Collectors.toList());
    }

    private static List<String> poolCodes(CardCheckReport r) {
        return r.poolErrors().stream().map(PoolError::code).collect(Collectors.toList());
    }

    // ---- the base set ----

    @Test
    void theWholeBaseSetPassesWithThePackPoolsOfTheShop() {
        CardCheckReport res = check(request("RESISTENZA_UMANA", RES, shopPools(RES_IDS)));
        assertEquals(List.of(), res.cardErrors());
        assertEquals(List.of(), res.poolErrors());
        assertTrue(res.ok());
        CardCheckReport ai = check(request("POTENZE_ARTIFICIALI", AI, shopPools(AI_IDS)));
        assertEquals(List.of(), ai.cardErrors());
        assertEquals(List.of(), ai.poolErrors());
    }

    @Test
    void theReportNamesTheEngineAndTheRulePack() {
        CardCheckReport r = check(request("RESISTENZA_UMANA", RES, null));
        assertEquals(EngineVersion.VERSION, r.engineVersion());
        assertEquals(ENGINE.rules().rulesetVersion(), r.rulesetVersion());
        assertEquals(ENGINE.rulesetHash(), r.rulesetHash());
    }

    @Test
    void sameRequestSameBytes() {
        String request = res(List.of(creature("A", 5, "{\"id\":\"K_LETALE\"}"), creature("B", 5, "")));
        assertEquals(ENGINE.cardCheck(request), ENGINE.cardCheck(request));
        assertTrue(ENGINE.cardCheck(request).endsWith("}\n"));
    }

    // ---- one card ----

    @Test
    void aKeywordOnARankItIsNotAllowedOnIsRejected() {
        CardCheckReport r = check(res(List.of(creature("A", 5, "{\"id\":\"K_LETALE\"}"))));
        assertEquals(List.of(CardCheckCodes.CARD_RANK_NOT_ALLOWED), cardCodes(r));
        assertEquals("A", r.cardErrors().get(0).cardId());
        assertEquals("cards[0]", r.cardErrors().get(0).path());
        assertFalse(r.ok());
        assertEquals(List.of(), cardCodes(check(res(List.of(creature("A", 1, "{\"id\":\"K_LETALE\"}"))))));
    }

    @Test
    void anEvolutionOnARankItIsNotAllowedOnIsRejected() {
        assertEquals(List.of(CardCheckCodes.CARD_RANK_NOT_ALLOWED),
                cardCodes(check(res(List.of(creature("A", 5, "{\"id\":\"EVO_CARNEFICE\"}"))))));
    }

    @Test
    void tooManyKeywordsAndTooManyEvolutionsAreRejected() {
        String four = "{\"id\":\"K_TRABOCCANTE\"},{\"id\":\"K_INSAZIABILE\"},{\"id\":\"K_RIGENERANTE\"},{\"id\":\"K_CONFINE\"}";
        assertEquals(List.of(CardCheckCodes.CARD_TOO_MANY_KEYWORDS), cardCodes(check(res(List.of(creature("A", 5, four))))));
        String two = "{\"id\":\"EVO_CRESCITA\"},{\"id\":\"EVO_TRINCEA\"}";
        assertEquals(List.of(CardCheckCodes.CARD_TOO_MANY_EVOLUTIONS), cardCodes(check(res(List.of(creature("A", 5, two))))));
    }

    @Test
    void anAbilityListedTwiceIsRejected() {
        assertEquals(List.of(CardCheckCodes.CARD_DUPLICATE_ABILITY),
                cardCodes(check(res(List.of(creature("A", 5, "{\"id\":\"K_CONFINE\"},{\"id\":\"K_CONFINE\"}"))))));
    }

    @Test
    void anAbilityTheEngineCannotBindIsRejected() {
        assertEquals(List.of(CardCheckCodes.CARD_BIND_FAILED),
                cardCodes(check(res(List.of(creature("A", 5, "{\"id\":\"K_NON_ESISTE\"}"))))), "unknown ability");
        assertEquals(List.of(CardCheckCodes.CARD_BIND_FAILED),
                cardCodes(check(res(List.of(creature("A", 5, "{\"id\":\"K_SCUDO\"}"))))), "missing required param x");
        assertEquals(List.of(CardCheckCodes.CARD_BIND_FAILED),
                cardCodes(check(res(List.of(creature("A", 5, "{\"id\":\"K_CONFINE\",\"params\":{\"nope\":1}}"))))),
                "unknown param");
        assertEquals(List.of(), cardCodes(check(res(List.of(creature("A", 5, "{\"id\":\"K_SCUDO\",\"params\":{\"x\":10}}"))))));
    }

    @Test
    void aCardOfTheOtherFactionIsRejected() {
        CardCheckReport r = check(request("POTENZE_ARTIFICIALI", List.of(creature("A", 5, "")), null));
        assertEquals(List.of(CardCheckCodes.CARD_WRONG_FACTION), cardCodes(r));
    }

    @Test
    void aCardThatBreaksTheSchemaIsRejectedWithItsId() {
        String bad = "{\"id\":\"X\",\"kind\":\"CREATURE\",\"rank\":5,\"suit\":\"VINCOLO\",\"subfaction\":\"ADATTATI\","
                + "\"atk\":1,\"def\":1,\"abilities\":[],\"colour\":\"red\"}";
        CardCheckReport r = check(res(List.of(bad)));
        assertEquals(List.of(CardCheckCodes.CARD_INVALID), cardCodes(r));
        assertEquals("X", r.cardErrors().get(0).cardId());
        assertTrue(r.cardErrors().get(0).message().startsWith("cards[0]"));
        CardCheckReport noId = check(res(List.of("{}")));
        assertEquals(List.of(CardCheckCodes.CARD_INVALID), cardCodes(noId));
        assertEquals(null, noId.cardErrors().get(0).cardId());
    }

    @Test
    void everyProblemOfACardIsReportedAndTheOthersAreStillChecked() {
        String letaleTwiceOnA5 = creature("A", 5, "{\"id\":\"K_LETALE\"},{\"id\":\"K_LETALE\"}");
        CardCheckReport r = check(res(List.of(letaleTwiceOnA5, creature("B", 5, "{\"id\":\"K_NON_ESISTE\"}"))));
        // card A: the first K_LETALE is on the wrong rank; the second is a duplicate and also on the wrong rank
        assertEquals(List.of(CardCheckCodes.CARD_RANK_NOT_ALLOWED, CardCheckCodes.CARD_DUPLICATE_ABILITY,
                CardCheckCodes.CARD_RANK_NOT_ALLOWED, CardCheckCodes.CARD_BIND_FAILED), cardCodes(r));
        assertEquals("B", r.cardErrors().get(3).cardId());
        assertEquals("cards[1]", r.cardErrors().get(3).path());
    }

    @Test
    void aKeywordOnASpellIsRejectedByTheBind() {
        String spell = "{\"id\":\"S\",\"kind\":\"SPELL\",\"faction\":\"RESISTENZA_UMANA\",\"abilities\":[{\"id\":\"K_CONFINE\"}]}";
        assertEquals(List.of(CardCheckCodes.CARD_BIND_FAILED), cardCodes(check(res(List.of(spell)))));
    }

    // ---- the pool ----

    @Test
    void anEmptyPoolAndADuplicateIdAreRejected() {
        assertEquals(List.of(CardCheckCodes.POOL_EMPTY), poolCodes(check(res(List.of()))));
        CardCheckReport r = check(res(List.of(creature("A", 5, ""), creature("A", 6, ""))));
        assertEquals(List.of(CardCheckCodes.POOL_DUPLICATE_CARD_ID), poolCodes(r));
        assertEquals("A", r.poolErrors().get(0).subject());
        assertEquals(List.of(), cardCodes(r), "the first card is kept and checked");
    }

    @Test
    void aPackPoolMustBeFillable() {
        String spell = "{\"id\":\"S\",\"kind\":\"SPELL\",\"faction\":\"RESISTENZA_UMANA\",\"abilities\":[]}";
        List<String> cards = List.of(creature("C5", 5, ""), creature("C9", 9, ""));
        String noAce = pool("elite", List.of("C5", "C9"), "{\"TIER_2\":2,\"TIER_4\":1}", 0);
        CardCheckReport r = check(request("RESISTENZA_UMANA", cards, noAce));
        assertEquals(List.of(CardCheckCodes.PACK_NO_CREATURE_FOR_TIER), poolCodes(r));
        assertEquals("TIER_4", r.poolErrors().get(0).subject());
        assertEquals("elite", r.poolErrors().get(0).pool());

        String noSupport = pool("standard", List.of("C5", "C9"), "{\"TIER_1\":1}", 2);
        assertEquals(List.of(CardCheckCodes.PACK_NO_SUPPORT_CARD),
                poolCodes(check(request("RESISTENZA_UMANA", cards, noSupport))));

        String unknown = pool("standard", List.of("C5", "GHOST"), "{\"TIER_1\":1}", 0);
        assertEquals(List.of(CardCheckCodes.PACK_UNKNOWN_CARD),
                poolCodes(check(request("RESISTENZA_UMANA", cards, unknown))));

        List<String> withSpell = new ArrayList<>(cards);
        withSpell.add(spell);
        assertTrue(check(request("RESISTENZA_UMANA", withSpell,
                pool("standard", List.of("C5", "C9", "S"), "{\"TIER_1\":1,\"TIER_2\":1}", 2))).ok());
    }

    @Test
    void tiersFollowTheRank() {
        assertEquals("TIER_4", CardPoolChecker.tierOf(1));
        assertEquals("TIER_1", CardPoolChecker.tierOf(2));
        assertEquals("TIER_1", CardPoolChecker.tierOf(6));
        assertEquals("TIER_2", CardPoolChecker.tierOf(7));
        assertEquals("TIER_2", CardPoolChecker.tierOf(10));
        assertEquals("TIER_3", CardPoolChecker.tierOf(11));
        assertEquals("TIER_3", CardPoolChecker.tierOf(13));
    }

    // ---- a broken request is not a report ----

    @Test
    void aBrokenRequestIsAMatchInputException() {
        String card = creature("A", 5, "");
        for (String bad : List.of(
                "{not json",
                "[]",
                "{\"schema_version\":2,\"faction\":\"RESISTENZA_UMANA\",\"cards\":[]}",
                "{\"schema_version\":1,\"faction\":\"NESSUNA\",\"cards\":[]}",
                "{\"schema_version\":1,\"faction\":\"RESISTENZA_UMANA\"}",
                "{\"schema_version\":1,\"faction\":\"RESISTENZA_UMANA\",\"cards\":[],\"extra\":1}",
                "{\"schema_version\":1,\"faction\":\"RESISTENZA_UMANA\",\"cards\":{}}",
                request("RESISTENZA_UMANA", List.of(card), "{\"name\":\"p\",\"card_ids\":[\"A\"]}"),
                request("RESISTENZA_UMANA", List.of(card), pool("p", List.of("A"), "{\"TIER_9\":1}", 0)),
                request("RESISTENZA_UMANA", List.of(card), pool("p", List.of("A"), "{\"TIER_1\":-1}", 1)),
                request("RESISTENZA_UMANA", List.of(card),
                        pool("p", List.of("A"), "{\"TIER_1\":1}", 0) + "," + pool("p", List.of("A"), "{\"TIER_1\":1}", 0)))) {
            assertThrows(MatchInputException.class, () -> check(bad), bad);
        }
    }

    /** The three pack types of application.yml (standard, advanced, elite) as pack pools over the whole faction. */
    private static String shopPools(List<String> ids) {
        return pool("standard", ids, "{\"TIER_1\":2,\"TIER_2\":2}", 2) + ","
                + pool("advanced", ids, "{\"TIER_1\":2,\"TIER_2\":2,\"TIER_3\":1}", 2) + ","
                + pool("elite", ids, "{\"TIER_2\":2,\"TIER_3\":2,\"TIER_4\":1}", 3);
    }

    private static String pool(String name, List<String> ids, String tierSlots, int support) {
        return "{\"name\":\"" + name + "\",\"card_ids\":[" + ids.stream().map(i -> "\"" + i + "\"")
                .collect(Collectors.joining(",")) + "],\"tier_slots\":" + tierSlots + ",\"support_slots\":" + support + "}";
    }
}
