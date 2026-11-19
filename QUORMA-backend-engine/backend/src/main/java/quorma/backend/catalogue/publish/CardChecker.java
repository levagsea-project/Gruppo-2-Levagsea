package quorma.backend.catalogue.publish;

import tools.jackson.databind.JsonNode;

/**
 * Checks a candidate card pool of one faction against the rules of the running engine ({@code POST /v1/cards/check}
 * of the engine-service, contract in {@code engine-service/AGENTS.md}). The engine is the single authority on what a
 * card may be: the backend only builds the request and reads the report.
 */
public interface CardChecker {

    /**
     * @param request {@code {schema_version, faction, cards, pools?}}
     * @return the engine's report {@code {ok, engine_version, ruleset_version, ruleset_hash, card_errors, pool_errors}}
     * @throws quorma.backend.engine.EngineUnavailableException if the engine service cannot be reached
     * @throws quorma.backend.engine.EngineRejectedException    if the engine refuses the request itself
     */
    JsonNode check(JsonNode request);
}
