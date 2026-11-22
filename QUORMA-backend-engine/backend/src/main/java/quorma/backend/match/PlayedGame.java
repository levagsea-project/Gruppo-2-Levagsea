package quorma.backend.match;

import java.util.regex.Pattern;
import quorma.backend.engine.EngineUnavailableException;
import quorma.backend.engine.MatchLog;
import tools.jackson.databind.JsonNode;

/**
 * What the backend keeps from one game the engine played: the header values that go in the replay recipe, the
 * outcome, and the {@code result_hash} the engine wrote. The log itself is not stored (2026-11-19-result-hash-from-engine).
 * A log that lacks any of them, or has a hash that is not 64 lowercase hex characters, is refused as an engine fault.
 */
record PlayedGame(long seed, String rulesetVersion, String rulesetHash, String engineVersion, String prngName,
                  String shuffleAlgorithm, String resultHash, MatchOutcome outcome, JsonNode log) {

    private static final Pattern SHA256 = Pattern.compile("^[0-9a-f]{64}$");

    static PlayedGame parse(MatchLog matchLog, long seed) {
        JsonNode doc = matchLog.document();
        JsonNode header = doc.path("header");
        String outcome = text(doc.path("result"), "outcome", 8);
        MatchOutcome parsed;
        try {
            parsed = MatchOutcome.valueOf(outcome);
        } catch (IllegalArgumentException e) {
            throw new EngineUnavailableException("the match log has an unknown outcome", e);
        }
        return new PlayedGame(seed,
                text(header, "ruleset_version", 32),
                hash(header, "ruleset_hash"),
                text(header, "engine_version", 32),
                text(header, "prng", 32),
                text(header, "shuffle", 32),
                hash(doc, "result_hash"),
                parsed,
                doc);
    }

    private static String text(JsonNode parent, String field, int maxLength) {
        JsonNode n = parent.path(field);
        if (!n.isString() || n.asString().isBlank() || n.asString().length() > maxLength) {
            throw new EngineUnavailableException("the match log has no usable " + field);
        }
        return n.asString();
    }

    private static String hash(JsonNode parent, String field) {
        JsonNode n = parent.path(field);
        if (!n.isString() || !SHA256.matcher(n.asString()).matches()) {
            throw new EngineUnavailableException("the match log has no valid " + field);
        }
        return n.asString();
    }
}
