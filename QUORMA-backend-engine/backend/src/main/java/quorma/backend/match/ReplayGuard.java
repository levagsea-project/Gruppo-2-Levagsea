package quorma.backend.match;

import java.util.Map;
import org.springframework.stereotype.Component;
import quorma.backend.common.ErrorCodes;
import quorma.backend.engine.EngineInfo;
import quorma.backend.engine.EngineInfoSource;

/**
 * Replay safety: a match is stored as a recipe (decks, seed, ruleset and engine versions, the SHA-256 of the result), and
 * a replay recomputes it. That is only meaningful on the engine build and the rule pack that played it, so before any
 * replay, call {@link #requireSameEngine}: it compares what the match recorded with what the running engine says it is
 * and fails with a clear, typed error on a mismatch instead of producing another match that merely looks like a replay.
 *
 * <p>The engine enforces the same thing from its side (a match input that names {@code engine_version} and
 * {@code ruleset_hash} is refused by the engine if they are not its own), so a replay that sends the stored values is
 * safe even without this check. This check comes first because it costs one small request, names both sides in the
 * error, and does not run a match to find out.
 */
@Component
public class ReplayGuard {

    private final EngineInfoSource engine;

    public ReplayGuard(EngineInfoSource engine) {
        this.engine = engine;
    }

    /**
     * @param match the stored match (its engine version, ruleset version and rule pack hash)
     * @throws ReplayUnavailableException 409 {@code replay.engine_version_mismatch} or {@code replay.ruleset_mismatch}
     * @throws quorma.backend.engine.EngineUnavailableException if the running engine cannot say what it is
     */
    public void requireSameEngine(Match match) {
        requireSameEngine(match.getEngineVersion(), match.getRulesetVersion(), match.getRulesetHash());
    }

    /** The same check on the three recorded values. */
    public void requireSameEngine(String engineVersion, String rulesetVersion, String rulesetHash) {
        EngineInfo running = engine.info();
        Map<String, String> stored = Map.of("engineVersion", engineVersion, "rulesetVersion", rulesetVersion,
                "rulesetHash", rulesetHash);
        Map<String, String> now = Map.of("engineVersion", running.engineVersion(), "rulesetVersion", running.rulesetVersion(),
                "rulesetHash", running.rulesetHash());
        if (!engineVersion.equals(running.engineVersion())) {
            throw new ReplayUnavailableException(ErrorCodes.REPLAY_ENGINE_VERSION_MISMATCH, stored, now);
        }
        if (!rulesetVersion.equals(running.rulesetVersion()) || !rulesetHash.equals(running.rulesetHash())) {
            throw new ReplayUnavailableException(ErrorCodes.REPLAY_RULESET_MISMATCH, stored, now);
        }
    }
}
