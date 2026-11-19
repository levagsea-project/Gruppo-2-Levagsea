package quorma.backend.match;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;
import quorma.backend.deck.DeckEligibility;
import quorma.backend.user.Role;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;

/**
 * Challenges by nickname. The challenger locks one deck per faction when creating; the opponent, who may be offline
 * for days, picks theirs when accepting, and accepting plays the whole series at once. Every error is a
 * {@link ChallengeException} with a code, except the engine's, which roll everything back.
 */
@Service
public class ChallengeService {

    private final ChallengeRepository challenges;
    private final ChallengeDeckRepository challengeDecks;
    private final UserRepository users;
    private final DeckEligibility eligibility;
    private final DeckSnapshotService snapshots;
    private final SeriesRunner runner;
    private final SeriesViewService views;
    private final MatchSeriesRepository seriesRepository;
    private final MatchProperties props;

    public ChallengeService(ChallengeRepository challenges, ChallengeDeckRepository challengeDecks,
                            UserRepository users, DeckEligibility eligibility, DeckSnapshotService snapshots,
                            SeriesRunner runner, SeriesViewService views, MatchSeriesRepository seriesRepository, MatchProperties props) {
        this.challenges = challenges;
        this.challengeDecks = challengeDecks;
        this.users = users;
        this.eligibility = eligibility;
        this.snapshots = snapshots;
        this.runner = runner;
        this.views = views;
        this.seriesRepository = seriesRepository;
        this.props = props;
    }

    /** Creates a PENDING challenge to the player with that nickname and locks the challenger's decks. */
    @Transactional
    public ChallengeResponse create(UUID challengerId, CreateChallengeRequest req) {
        User opponent = users.findByUsernameIgnoreCase(req.opponentUsername().strip())
                .filter(u -> u.getRole() == Role.PLAYER)
                .orElseThrow(() -> new ChallengeException(HttpStatus.NOT_FOUND, ErrorCodes.CHALLENGE_OPPONENT_NOT_FOUND));
        if (opponent.getId().equals(challengerId)) {
            throw new ChallengeException(HttpStatus.BAD_REQUEST, ErrorCodes.CHALLENGE_SELF);
        }
        eligibility.requireCanPlay(challengerId);
        if (challenges.existsByChallengerIdAndOpponentIdAndStatus(challengerId, opponent.getId(), ChallengeStatus.PENDING)) {
            throw new ChallengeException(HttpStatus.CONFLICT, ErrorCodes.CHALLENGE_ALREADY_PENDING);
        }
        Map<Faction, LockedDeck> mine = snapshots.lockChoices(challengerId, req.decks());

        String code = CodeGenerator.unique(CodeGenerator.CHALLENGE_PREFIX, challenges::existsByCode);
        Challenge challenge = new Challenge(challengerId, opponent.getId(), code, req.startingFaction(),
                SeriesFormat.FACTION_SWAP, ChallengeStatus.PENDING, Instant.now().plus(props.challengeTtl()));
        try {
            challenge = challenges.saveAndFlush(challenge); // the unique index allows one pending challenge per pair
        } catch (DataIntegrityViolationException e) {
            throw new ChallengeException(HttpStatus.CONFLICT, ErrorCodes.CHALLENGE_ALREADY_PENDING);
        }
        saveDecks(challenge.getId(), ChallengeSide.CHALLENGER, mine);
        return response(challenge);
    }

    /**
     * The opponent accepts: they pick their decks and the whole series is played now. The challenge row is locked, so a
     * second accept waits and then finds it no longer PENDING. An expired challenge is marked EXPIRED and stays so
     * ({@code noRollbackFor}); an engine fault rolls everything back and the challenge stays PENDING.
     */
    @Transactional(noRollbackFor = ChallengeException.class)
    public SeriesResponse accept(UUID opponentId, UUID challengeId, List<DeckChoice> choices) {
        Challenge challenge = lockFor(challengeId, opponentId, true);
        requirePending(challenge);
        eligibility.requireCanPlay(opponentId);
        Map<Faction, LockedDeck> mine = snapshots.lockChoices(opponentId, choices);
        Map<Faction, LockedDeck> theirs = storedDecks(challenge.getId(), ChallengeSide.CHALLENGER);
        saveDecks(challenge.getId(), ChallengeSide.OPPONENT, mine);

        SeriesRunner.SeriesResult result = runner.run(SeriesKind.CHALLENGE, challenge.getId(), challenge.getChallengerId(),
                opponentId, Faction.valueOf(challenge.getChallengerFaction()), theirs, mine, opponentId);

        challenge.setStatus(ChallengeStatus.ACCEPTED);
        challenge.setResolvedAt(Instant.now());
        challenges.saveAndFlush(challenge);
        Map<UUID, tools.jackson.databind.JsonNode> logs = result.games().stream()
                .collect(Collectors.toMap(g -> g.match().getId(), SeriesRunner.PlayedMatch::log));
        return views.view(opponentId, result.series(), result.games().stream().map(SeriesRunner.PlayedMatch::match).toList(), logs);
    }

    /** The opponent turns the challenge down. */
    @Transactional(noRollbackFor = ChallengeException.class)
    public ChallengeResponse decline(UUID opponentId, UUID challengeId) {
        Challenge challenge = lockFor(challengeId, opponentId, true);
        requirePending(challenge);
        return close(challenge, ChallengeStatus.DECLINED);
    }

    /** The challenger withdraws a challenge nobody answered yet. */
    @Transactional(noRollbackFor = ChallengeException.class)
    public ChallengeResponse cancel(UUID challengerId, UUID challengeId) {
        Challenge challenge = lockFor(challengeId, challengerId, false);
        requirePending(challenge);
        return close(challenge, ChallengeStatus.CANCELLED);
    }

    /** One of my challenges (sent or received); 404 for anyone else. */
    @Transactional(readOnly = true)
    public ChallengeResponse get(UUID userId, UUID challengeId) {
        Challenge challenge = challenges.findById(challengeId)
                .filter(c -> c.getChallengerId().equals(userId) || c.getOpponentId().equals(userId))
                .orElseThrow(() -> new ChallengeException(HttpStatus.NOT_FOUND, ErrorCodes.CHALLENGE_NOT_FOUND));
        return response(challenge);
    }

    /** The challenges I sent or received, newest first, optionally only with one status. */
    @Transactional(readOnly = true)
    public List<ChallengeResponse> list(UUID userId, Optional<ChallengeStatus> status) {
        return challenges.findOfPlayer(userId).stream()
                .map(this::response)
                .filter(r -> status.isEmpty() || r.status() == status.get())
                .toList();
    }

    // -- internals ---------------------------------------------------------------------------------------

    /** Loads and locks the challenge; 404 unless {@code userId} is its opponent ({@code asOpponent}) or challenger. */
    private Challenge lockFor(UUID challengeId, UUID userId, boolean asOpponent) {
        return challenges.findByIdForUpdate(challengeId)
                .filter(c -> (asOpponent ? c.getOpponentId() : c.getChallengerId()).equals(userId))
                .orElseThrow(() -> new ChallengeException(HttpStatus.NOT_FOUND, ErrorCodes.CHALLENGE_NOT_FOUND));
    }

    /** 409 unless PENDING; a pending one past its expiry is marked EXPIRED (kept) and refused. */
    private void requirePending(Challenge challenge) {
        if (challenge.getStatus() != ChallengeStatus.PENDING) {
            throw new ChallengeException(HttpStatus.CONFLICT, ErrorCodes.CHALLENGE_NOT_PENDING);
        }
        if (!challenge.getExpiresAt().isAfter(Instant.now())) {
            close(challenge, ChallengeStatus.EXPIRED);
            throw new ChallengeException(HttpStatus.CONFLICT, ErrorCodes.CHALLENGE_EXPIRED);
        }
    }

    private ChallengeResponse close(Challenge challenge, ChallengeStatus status) {
        challenge.setStatus(status);
        challenge.setResolvedAt(Instant.now());
        return response(challenges.saveAndFlush(challenge));
    }

    private void saveDecks(UUID challengeId, ChallengeSide side, Map<Faction, LockedDeck> decks) {
        decks.forEach((faction, d) -> challengeDecks.save(new ChallengeDeck(
                new ChallengeDeckId(challengeId, side.name(), faction.name()), d.poolSnapshotId(), d.deck())));
        challengeDecks.flush();
    }

    private Map<Faction, LockedDeck> storedDecks(UUID challengeId, ChallengeSide side) {
        Map<Faction, LockedDeck> map = new EnumMap<>(Faction.class);
        for (ChallengeDeck d : challengeDecks.findByChallengeId(challengeId)) {
            if (d.getId().side().equals(side.name())) {
                Faction faction = Faction.valueOf(d.getId().faction());
                map.put(faction, new LockedDeck(faction, d.getPoolSnapshotId(), d.getDeck()));
            }
        }
        return map;
    }

    private ChallengeResponse response(Challenge c) {
        Map<UUID, UserRef> refs = views.refs(List.of(c.getChallengerId(), c.getOpponentId()));
        ChallengeStatus shown = c.getStatus() == ChallengeStatus.PENDING && !c.getExpiresAt().isAfter(Instant.now())
                ? ChallengeStatus.EXPIRED : c.getStatus();
        UUID seriesId = c.getStatus() == ChallengeStatus.ACCEPTED
                ? seriesRepository.findByChallengeId(c.getId()).map(MatchSeries::getId).orElse(null) : null;
        return new ChallengeResponse(c.getId(), c.getCode(), shown, refs.get(c.getChallengerId()),
                refs.get(c.getOpponentId()), c.getChallengerFaction(), c.getCreatedAt(), c.getExpiresAt(),
                c.getResolvedAt(), seriesId);
    }
}
