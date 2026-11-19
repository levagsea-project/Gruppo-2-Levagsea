package quorma.backend.engine;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Which engine the engine service is running, as {@code GET /engine/info} says it: the same values the header of every
 * MatchLog it writes names. A replay is only valid on the engine and the rule pack that played the match.
 *
 * @param engineVersion  the engine build, for example {@code 1.0.0-SNAPSHOT}
 * @param rulesetVersion the ruleset it plays, for example {@code 1.0-alpha}
 * @param rulesetHash    SHA-256 of the rule pack it plays
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EngineInfo(String engineVersion, String rulesetVersion, String rulesetHash) {
}
