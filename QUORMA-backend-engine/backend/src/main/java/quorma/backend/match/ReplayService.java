package quorma.backend.match;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import quorma.backend.catalogue.Faction;
import quorma.backend.catalogue.PoolSnapshotRepository;
import quorma.backend.common.ErrorCodes;
import quorma.backend.engine.HttpGameEngine;
import quorma.backend.engine.MatchLog;
import tools.jackson.databind.node.ObjectNode;

/**
 * Plays a stored game again and proves it is the same game. The log is not stored (2026-11-19-result-hash-from-engine):
 * the recipe is, and the engine rebuilds the log from it. The steps, in this order:
 * <ol>
 *   <li>only a player of the series can ask (404 for anyone else);</li>
 *   <li>{@link ReplayGuard} refuses with 409 if the running engine is not the build and the rule pack that played the
 *       game, before any match is run;</li>
 *   <li>the engine plays the stored input again (the same builder the series used, plus the stored engine version and
 *       rule pack hash, which the engine itself checks too);</li>
 *   <li>the {@code result_hash} of the new log must equal the stored one.</li>
 * </ol>
 * Not one transaction: the engine call can take a while and must not hold a database connection.
 */
@Service
public class ReplayService {

    private static final Logger log = LoggerFactory.getLogger(ReplayService.class);

    private final MatchService matchService;
    private final MatchRepository matches;
    private final PoolSnapshotRepository snapshots;
    private final ReplayGuard guard;
    private final HttpGameEngine engine;

    public ReplayService(MatchService matchService, MatchRepository matches, PoolSnapshotRepository snapshots,
                         ReplayGuard guard, HttpGameEngine engine) {
        this.matchService = matchService;
        this.matches = matches;
        this.snapshots = snapshots;
        this.guard = guard;
        this.engine = engine;
    }

    public ReplayResponse replay(UUID userId, UUID seriesId, int gameNumber) {
        matchService.mine(userId, seriesId);
        Match match = matches.findBySeriesIdOrderByGameNumber(seriesId).stream()
                .filter(m -> m.getGameNumber() == gameNumber)
                .findFirst()
                .orElseThrow(() -> new ChallengeException(HttpStatus.NOT_FOUND, ErrorCodes.MATCH_NOT_FOUND));

        PlayedGame played = rebuild(match);
        return new ReplayResponse(seriesId, gameNumber, match.getId(), factionOf(match.getPoolSnapshotAId()).name(),
                factionOf(match.getPoolSnapshotBId()).name(), match.getOutcome(), match.getResultHash(), true,
                played.log());
    }

    /**
     * Plays a stored game again and checks it: the guard first, then the engine, then the result hash and outcome.
     * No user check (the caller decides who may see it). Used by the replay endpoint and by the telemetry catch-up.
     */
    PlayedGame rebuild(Match match) {
        guard.requireSameEngine(match);

        Faction factionA = factionOf(match.getPoolSnapshotAId());
        Faction factionB = factionOf(match.getPoolSnapshotBId());
        ObjectNode input = (ObjectNode) SeriesRunner.input(match.getSeed(), match.getRulesetVersion(),
                new LockedDeck(factionA, match.getPoolSnapshotAId(), match.getDeckA()),
                new LockedDeck(factionB, match.getPoolSnapshotBId(), match.getDeckB()));
        input.put("engine_version", match.getEngineVersion());
        input.put("ruleset_hash", match.getRulesetHash());

        MatchLog replayed = engine.simulate(input);
        PlayedGame played = PlayedGame.parse(replayed, match.getSeed());
        if (!played.resultHash().equals(match.getResultHash()) || played.outcome() != match.getOutcome()) {
            // Same engine and same rule pack, yet another game: a determinism bug or a damaged recipe. Never hide it.
            log.error("Replay of match {} (series {}, game {}) differs from the stored game: hash {} vs {}, outcome {} vs {}",
                    match.getId(), match.getSeriesId(), match.getGameNumber(), played.resultHash(), match.getResultHash(),
                    played.outcome(), match.getOutcome());
            throw new ChallengeException(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.REPLAY_RESULT_MISMATCH);
        }
        return played;
    }

    private Faction factionOf(UUID poolSnapshotId) {
        return Faction.valueOf(snapshots.findById(poolSnapshotId).orElseThrow().getFaction());
    }
}
