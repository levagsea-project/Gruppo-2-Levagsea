package quorma.gameengine.cards;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import quorma.gameengine.json.CanonicalJson;

/**
 * The result of checking a card pool ({@link CardPoolChecker}): what the engine that ran the check is, and every
 * problem found, in input order. A pool with no problem is {@link #ok()}.
 *
 * <p>Codes are stable identifiers for the caller to translate; {@code message} is the engine's own English text, for
 * logs only (the player never sees it).
 *
 * @param engineVersion the engine that checked the pool
 * @param rulesetVersion the ruleset it checked against
 * @param rulesetHash   the hash of the rule pack it checked against
 * @param cardErrors    problems of one card, in card order
 * @param poolErrors    problems of the pool as a whole or of one pack pool
 */
public record CardCheckReport(
        String engineVersion, String rulesetVersion, String rulesetHash, List<CardError> cardErrors,
        List<PoolError> poolErrors) {

    /** Version of the request and report format. */
    public static final int SCHEMA_VERSION = 1;

    public CardCheckReport {
        cardErrors = List.copyOf(cardErrors);
        poolErrors = List.copyOf(poolErrors);
    }

    /** True when no card and no pool has a problem. */
    public boolean ok() {
        return cardErrors.isEmpty() && poolErrors.isEmpty();
    }

    /**
     * A problem of one card.
     *
     * @param cardId the card's {@code id}, or null if the card has none that can be read
     * @param code   one of {@link CardCheckCodes}
     * @param path   where in the request the card is, for example {@code cards[12]}
     */
    public record CardError(String cardId, String code, String path, String message) {
    }

    /**
     * A problem of the pool as a whole, or of one pack pool.
     *
     * @param pool    the name of the pack pool, or null for a problem of the whole pool
     * @param code    one of {@link CardCheckCodes}
     * @param subject the card id or the Tier the problem is about, or null
     */
    public record PoolError(String pool, String code, String subject, String message) {
    }

    /** The canonical JSON of the report, trailing newline included. */
    public String toJson() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schema_version", SCHEMA_VERSION);
        root.put("ok", ok());
        root.put("engine_version", engineVersion);
        root.put("ruleset_version", rulesetVersion);
        root.put("ruleset_hash", rulesetHash);
        List<Object> cards = new ArrayList<>();
        for (CardError e : cardErrors) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("card_id", e.cardId());
            m.put("code", e.code());
            m.put("path", e.path());
            m.put("message", e.message());
            cards.add(m);
        }
        root.put("card_errors", cards);
        List<Object> pools = new ArrayList<>();
        for (PoolError e : poolErrors) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("pool", e.pool());
            m.put("code", e.code());
            m.put("subject", e.subject());
            m.put("message", e.message());
            pools.add(m);
        }
        root.put("pool_errors", pools);
        return CanonicalJson.write(root);
    }
}
