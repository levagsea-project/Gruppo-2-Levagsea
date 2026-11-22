package quorma.gameengine.cards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import quorma.gameengine.GameEngine;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * The engine's card check ({@link CardPoolChecker}) is the single authority on what a card may be, and
 * {@code data/validate.py} (check 12) stays as the CI check of the set files: two implementations of the same rules, kept
 * honest by this test. It feeds the same cards, good and deliberately bad ({@code parity/cards.json}, plus a sample of
 * the real base set), to both and requires the same verdict. A case may record a known difference, and the test then
 * asserts exactly that difference, so it cannot move unnoticed. Skipped when no Python is installed.
 */
class CardCheckParityTest {

    private static final GameEngine ENGINE = new GameEngine();
    private static final JsonMapper MAPPER = JsonMapper.builder().build();
    private static final Path DATA = Path.of("..", "data");
    private static final Pattern ERROR_LINE = Pattern.compile("^ERROR sets/PAR\\.json (\\S+?)(?:[: \\[]).*$");

    private record Case(String id, String name, ObjectNode card, String faction, String engine, String validate, String why) {
    }

    private static String python() {
        for (String exe : List.of("python3", "python")) {
            try {
                Process p = new ProcessBuilder(exe, "--version").redirectErrorStream(true).start();
                p.getInputStream().readAllBytes();
                if (p.waitFor() == 0) {
                    return exe;
                }
            } catch (IOException | InterruptedException e) {
                // try the next name
            }
        }
        return null;
    }

    private static String factionOf(JsonNode card) {
        if (card.has("faction")) {
            return card.get("faction").asString();
        }
        String sub = card.path("subfaction").asString("");
        return List.of("ADATTATI", "SPEZZASIGILLI", "PORTANOME", "SEGNATERRA").contains(sub) ? "RESISTENZA_UMANA" : "POTENZE_ARTIFICIALI";
    }

    private static List<Case> cases() throws IOException {
        List<Case> out = new ArrayList<>();
        JsonNode fixture = MAPPER.readTree(Files.readString(Path.of("src", "test", "resources", "parity", "cards.json"), StandardCharsets.UTF_8));
        int n = 0;
        for (JsonNode c : fixture.get("cases")) {
            ObjectNode card = ((ObjectNode) c.get("card")).deepCopy();
            String faction = factionOf(card);
            String id = "PAR-" + (faction.equals("RESISTENZA_UMANA") ? "RES" : "AI") + "-" + (++n);
            card.put("id", id);
            JsonNode expect = c.get("expect");
            String engine = expect.isString() ? expect.asString() : expect.get("engine").asString();
            String validate = expect.isString() ? expect.asString() : expect.get("validate").asString();
            out.add(new Case(id, c.get("name").asString(), card, faction, engine, validate, expect.isString() ? null : expect.get("why").asString()));
        }
        // and real cards: the first creature of each faction, an inline-effect creature, a spell and an equipment of the base set
        JsonNode base = MAPPER.readTree(Files.readString(DATA.resolve("sets").resolve("BASE.json"), StandardCharsets.UTF_8));
        Set<String> wanted = new HashSet<>(List.of("creature RESISTENZA_UMANA", "creature POTENZE_ARTIFICIALI", "SPELL", "EQUIPMENT", "inline"));
        for (JsonNode c : base.get("cards")) {
            String kind = c.get("kind").asString();
            String faction = factionOf(c);
            String key = kind.equals("CREATURE") ? "creature " + faction : kind;
            boolean inline = kind.equals("CREATURE") && c.get("abilities").toString().contains("\"expansions\"");
            String taken = inline && wanted.contains("inline") ? "inline" : wanted.contains(key) ? key : null;
            if (taken != null) {
                wanted.remove(taken);
                ObjectNode card = ((ObjectNode) c).deepCopy();
                card.remove("sets");
                String id = "PAR-" + (faction.equals("RESISTENZA_UMANA") ? "RES" : "AI") + "-" + (++n);
                card.put("id", id);
                out.add(new Case(id, "a real card of the base set: " + c.get("id").asString(), card, faction, "ok", "ok", null));
            }
        }
        assertTrue(wanted.isEmpty(), "the base set should have all these kinds of card: " + wanted);
        return out;
    }

    private static boolean engineAccepts(Case c) {
        ObjectNode request = MAPPER.createObjectNode();
        request.put("schema_version", 1);
        request.put("faction", c.faction());
        ArrayNode cards = request.putArray("cards");
        cards.add(c.card());
        return ENGINE.checkCards(MAPPER.writeValueAsString(request)).cardErrors().isEmpty();
    }

    /** validate.py over a copy of data/ whose only set file holds all the cards: the ids with an error. */
    private static Set<String> validatorRejects(String python, List<Case> cases) throws IOException, InterruptedException {
        Path tmp = Files.createTempDirectory("parity");
        Files.walkFileTree(DATA, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.getFileName() != null && dir.getFileName().toString().equals("sets")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Files.createDirectories(tmp.resolve(DATA.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, tmp.resolve(DATA.relativize(file).toString()));
                return FileVisitResult.CONTINUE;
            }
        });
        ObjectNode set = MAPPER.createObjectNode();
        set.putArray("set_definitions").addObject().put("code", "PAR");
        ArrayNode cards = set.putArray("cards");
        ObjectNode it = set.putObject("texts").putObject("it");
        it.put("set.PAR.name", "Parita");
        it.put("set.PAR.description", "Carte per il confronto");
        for (Case c : cases) {
            ObjectNode card = c.card().deepCopy();
            card.putArray("sets").add("PAR");
            cards.add(card);
            it.put("card." + c.id() + ".name", "Carta " + c.id());
            it.put("card." + c.id() + ".flavour", "Testo " + c.id());
        }
        Files.createDirectories(tmp.resolve("sets"));
        Files.writeString(tmp.resolve("sets").resolve("PAR.json"), MAPPER.writeValueAsString(set), StandardCharsets.UTF_8);

        Process p = new ProcessBuilder(python, "-X", "utf8", DATA.resolve("validate.py").toString(), tmp.toString())
                .redirectErrorStream(true).start();
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        p.waitFor();
        Set<String> rejected = new HashSet<>();
        for (String line : output.split("\\R")) {
            Matcher m = ERROR_LINE.matcher(line);
            if (m.matches()) {
                rejected.add(m.group(1));
            } else if (line.startsWith("ERROR") && !line.startsWith("ERROR sets/PAR.json")) {
                throw new AssertionError("validate.py found a problem outside the cards under test: " + line);
            }
        }
        return rejected;
    }

    @Test
    void theEngineAndValidatePyJudgeTheSameCardsTheSameWay() throws Exception {
        String python = python();
        assumeTrue(python != null, "no Python on this machine: the parity with validate.py is not checked");
        List<Case> cases = cases();
        Set<String> validatorRejected = validatorRejects(python, cases);

        List<String> differences = new ArrayList<>();
        int bad = 0;
        for (Case c : cases) {
            boolean engineOk = engineAccepts(c);
            boolean validatorOk = !validatorRejected.contains(c.id());
            if (engineOk != c.engine().equals("ok")) {
                differences.add(c.name() + ": the engine " + (engineOk ? "accepts" : "rejects") + ", expected " + c.engine());
            }
            if (validatorOk != c.validate().equals("ok")) {
                differences.add(c.name() + ": validate.py " + (validatorOk ? "accepts" : "rejects") + ", expected " + c.validate());
            }
            if (!c.engine().equals(c.validate())) {
                assertTrue(c.why() != null && !c.why().isBlank(), "a known difference needs its reason: " + c.name());
            }
            if (c.engine().equals("reject") && c.validate().equals("reject")) {
                bad++;
            }
        }
        assertEquals(List.of(), differences);
        assertTrue(bad >= 18, "the bad cards the two must both reject: " + bad);
    }
}
