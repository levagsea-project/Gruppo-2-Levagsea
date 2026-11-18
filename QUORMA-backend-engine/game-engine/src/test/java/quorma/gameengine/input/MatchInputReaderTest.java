package quorma.gameengine.input;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.Faction;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.SpellCard;
import quorma.gameengine.model.Subfaction;

/** The match input JSON: what the schema allows is read, everything else is refused with its JSON path. */
class MatchInputReaderTest {

    private static final String CREATURE = """
            {"id": "RU-VIN-A", "kind": "CREATURE", "rank": 1, "suit": "VINCOLO", "subfaction": "ADATTATI",
             "atk": 4000, "def": 7000, "abilities": [
               {"id": "K_SCUDO", "params": {"x": 25, "kind": "pct"}},
               {"expansions": [{"trigger_id": "T19", "effect_id": "E1",
                 "effect_params": {"mode": "pct_base_atk", "permille": "$tune.K_INSAZIABILE.permille"},
                 "target_id": "S1", "duration": "D_Permanente", "class": "T", "max_stacks": 2}]}]}""";

    private static String input(String deckA) {
        return """
                {"schema_version": 1, "seed": -42, "ruleset_version": "1.0-alpha",
                 "ruleset_hash": "%s",
                 "players": {
                   "A": {"faction": "RESISTENZA_UMANA", "pool_snapshot_id": "pool-7", "deck": [%s]},
                   "B": {"faction": "POTENZE_ARTIFICIALI", "deck": [
                     {"id": "PA-S1", "kind": "SPELL", "faction": "POTENZE_ARTIFICIALI", "abilities": []},
                     {"id": "PA-E1", "kind": "EQUIPMENT", "faction": "POTENZE_ARTIFICIALI", "abilities": []}]}}}"""
                .formatted("ab".repeat(32), deckA);
    }

    private static String refused(String json) {
        return assertThrows(MatchInputException.class, () -> MatchInputReader.read(json)).getMessage();
    }

    @Test
    void readsEveryField() {
        MatchInput in = MatchInputReader.read(input(CREATURE));

        assertEquals(1, in.schemaVersion());
        assertEquals(-42L, in.seed());
        assertEquals("1.0-alpha", in.rulesetVersion());
        assertEquals(Optional.of("ab".repeat(32)), in.rulesetHash());
        assertEquals(Faction.RESISTENZA_UMANA, in.player(Side.A).faction());
        assertEquals(Optional.of("pool-7"), in.player(Side.A).poolSnapshotId());
        assertEquals(Optional.empty(), in.player(Side.B).poolSnapshotId());
        assertEquals(List.of(Side.A, Side.B), List.copyOf(in.players().keySet()));

        CreatureCard c = assertInstanceOf(CreatureCard.class, in.player(Side.A).deck().get(0));
        assertEquals("RU-VIN-A", c.id());
        assertEquals(1, c.rank());
        assertEquals(Suit.VINCOLO, c.suit());
        assertEquals(Subfaction.ADATTATI, c.subfaction());
        assertEquals(4000, c.atk());
        assertEquals(7000, c.def());
        assertEquals(new AbilityRef("K_SCUDO", Map.of("x", new Value.IntV(25), "kind", new Value.StrV("pct"))),
                c.abilities().get(0));
        ExpansionDef x = assertInstanceOf(InlineEffect.class, c.abilities().get(1)).expansions().get(0);
        assertEquals("E1", x.effectId());
        assertEquals(ResClass.T, x.cls());
        assertEquals(new Value.Ref(Value.RefKind.TUNE, "K_INSAZIABILE.permille"), x.effectParams().get("permille"));
        assertEquals(Map.of("max_stacks", new Value.IntV(2)), x.common());

        assertInstanceOf(SpellCard.class, in.player(Side.B).deck().get(0));
        assertInstanceOf(EquipmentCard.class, in.player(Side.B).deck().get(1));
    }

    @Test
    void theHashAndThePoolSnapshotAreOptional() {
        MatchInput in = MatchInputReader.read("""
                {"schema_version": 1, "seed": 0, "ruleset_version": "1.0-alpha", "players": {
                  "A": {"faction": "RESISTENZA_UMANA", "deck": [%s]},
                  "B": {"faction": "RESISTENZA_UMANA", "deck": [%s]}}}""".formatted(CREATURE, CREATURE));
        assertEquals(Optional.empty(), in.rulesetHash());
    }

    @Test
    void badJsonIsRefused() {
        assertTrue(refused("{").startsWith("input: invalid JSON"));
        assertTrue(refused("{\"seed\": 1, \"seed\": 2}").contains("invalid JSON"), "duplicate keys");
        assertTrue(refused("[]").contains("input: must be an object"));
    }

    @Test
    void unknownKeysAreRefusedAtEveryLevel() {
        assertTrue(refused(input(CREATURE).replace("\"seed\"", "\"extra\": 1, \"seed\"")).contains("input: unknown key 'extra'"));
        assertTrue(refused(input(CREATURE).replace("\"rank\"", "\"name\": \"x\", \"rank\""))
                .contains("players.A.deck[0]: unknown key 'name'"));
        assertTrue(refused(input(CREATURE).replace("\"max_stacks\"", "\"typo\": 1, \"max_stacks\""))
                .contains("players.A.deck[0].abilities[1].expansions[0]: unknown key 'typo'"));
    }

    @Test
    void fieldsAreChecked() {
        assertTrue(refused(input(CREATURE).replace("\"schema_version\": 1", "\"schema_version\": 2"))
                .contains("this engine reads version 1, got 2"));
        assertTrue(refused(input(CREATURE).replace("\"rank\": 1", "\"rank\": 14")).contains("players.A.deck[0].rank: must be 1..13"));
        assertTrue(refused(input(CREATURE).replace("\"def\": 7000", "\"def\": 0")).contains(".def: must be 1.."));
        assertTrue(refused(input(CREATURE).replace("\"VINCOLO\"", "\"Vincolo\"")).contains(".suit: must be one of [VINCOLO"));
        assertTrue(refused(input(CREATURE).replace("\"faction\": \"RESISTENZA_UMANA\"", "\"faction\": \"Resistenza Umana\""))
                .contains("players.A.faction: must be one of"));
        assertTrue(refused(input(CREATURE).replace("\"CREATURE\"", "\"TRAP\"")).contains(".kind: must be CREATURE, SPELL or EQUIPMENT"));
        assertTrue(refused(input(CREATURE).replace("\"ab", "\"AB")).contains("ruleset_hash: must be 64 lower-case hex"));
        assertTrue(refused(input(CREATURE).replace("\"seed\": -42", "\"seed\": 1.5")).contains("input.seed: must be an integer"));
        assertTrue(refused(input("")).contains("players.A.deck: must have 1 to 64 cards, got 0"));
    }

    @Test
    void abilitiesAreChecked() {
        assertTrue(refused(input(CREATURE).replace("\"K_SCUDO\"", "\"Scudo\"")).contains(".abilities[0].id: must look like K_NAME"));
        assertTrue(refused(input(CREATURE).replace("\"x\": 25", "\"x\": \"$param.y\""))
                .contains(".abilities[0].params.x: must be an integer, a string or a boolean (no $ references on a card)"));
        assertTrue(refused(input(CREATURE).replace("\"class\": \"T\"", "\"class\": \"Z\"")).contains("expansions[0].class"));
        assertTrue(refused(input(CREATURE).replace(", \"abilities\": [", ", \"abilitiez\": [")).contains("unknown key 'abilitiez'"));
    }
}
