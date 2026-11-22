package quorma.backend.match;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;
import quorma.backend.deck.DeckEligibility;
import quorma.backend.deck.DeckException;
import quorma.backend.user.UserRepository;
import tools.jackson.databind.JsonNode;

/**
 * Matchmaking: the system picks a random opponent and plays at once against their defense decks, even if they are
 * offline. The opponent gets the result at their next login. Only players who logged in within
 * {@code matches.matchmaking-max-offline} (at most 7 days) and have a legal defense deck for both factions are picked.
 */
@Service
public class MatchmakingService {

    private static final Logger LOG = LoggerFactory.getLogger(MatchmakingService.class);

    /** How many random players are looked at before giving up. */
    static final int CANDIDATES = 20;

    private final UserRepository users;
    private final DeckEligibility eligibility;
    private final DeckSnapshotService snapshots;
    private final SeriesRunner runner;
    private final SeriesViewService views;
    private final MatchProperties props;

    public MatchmakingService(UserRepository users, DeckEligibility eligibility, DeckSnapshotService snapshots,
                              SeriesRunner runner, SeriesViewService views, MatchProperties props) {
        this.users = users;
        this.eligibility = eligibility;
        this.snapshots = snapshots;
        this.runner = runner;
        this.views = views;
        this.props = props;
    }

    /** Picks an opponent and plays the whole series. 404 {@code matchmaking.no_opponent} when nobody can be picked. */
    @Transactional
    public SeriesResponse play(UUID attackerId, MatchmakingRequest req) {
        eligibility.requireCanPlay(attackerId);
        Map<Faction, LockedDeck> mine = snapshots.lockChoices(attackerId, req.decks());

        Instant since = Instant.now().minus(props.matchmakingMaxOffline());
        for (UUID candidate : users.findRecentPlayers(attackerId, since, CANDIDATES)) {
            if (!eligibility.status(candidate).canPlay()) {
                continue;
            }
            Map<Faction, LockedDeck> theirs;
            try {
                theirs = snapshots.lockDefense(candidate);
            } catch (DeckException e) {
                LOG.info("Matchmaking skipped a player whose defense decks cannot be used: {}", e.code());
                continue;
            }
            SeriesRunner.SeriesResult result = runner.run(SeriesKind.MATCHMAKING, null, attackerId, candidate,
                    Faction.valueOf(req.startingFaction()), mine, theirs, attackerId);
            Map<UUID, JsonNode> logs = result.games().stream()
                    .collect(Collectors.toMap(g -> g.match().getId(), SeriesRunner.PlayedMatch::log));
            List<Match> games = result.games().stream().map(SeriesRunner.PlayedMatch::match).toList();
            return views.view(attackerId, result.series(), games, logs);
        }
        throw new ChallengeException(HttpStatus.NOT_FOUND, ErrorCodes.MATCHMAKING_NO_OPPONENT);
    }
}
