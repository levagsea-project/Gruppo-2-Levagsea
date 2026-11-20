package quorma.backend.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;
import quorma.backend.deck.DeckEligibility;
import quorma.backend.deck.DeckException;
import quorma.backend.engine.EngineUnavailableException;
import quorma.backend.user.Role;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;

/** The rules around a challenge: who may do what, expiry, repeated accepts and engine faults. */
class ChallengeServiceTest {

    private final ChallengeRepository challenges = mock(ChallengeRepository.class);
    private final ChallengeDeckRepository challengeDecks = mock(ChallengeDeckRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final DeckEligibility eligibility = mock(DeckEligibility.class);
    private final DeckSnapshotService snapshots = mock(DeckSnapshotService.class);
    private final SeriesRunner runner = mock(SeriesRunner.class);
    private final SeriesViewService views = mock(SeriesViewService.class);
    private final MatchSeriesRepository seriesRepository = mock(MatchSeriesRepository.class);
    private final MatchProperties props = new MatchProperties(null, null, null);
    private final ChallengeService service = new ChallengeService(challenges, challengeDecks, users, eligibility,
            snapshots, runner, views, seriesRepository, props);

    private final UUID challengerId = UUID.randomUUID();
    private final UUID opponentId = UUID.randomUUID();
    private final UUID id = UUID.randomUUID();
    private final List<DeckChoice> choices = List.of(
            new DeckChoice("RESISTENZA_UMANA", UUID.randomUUID()), new DeckChoice("POTENZE_ARTIFICIALI", UUID.randomUUID()));

    private Challenge pending(Instant expiresAt) {
        return new Challenge(challengerId, opponentId, "ch-ABCD-EFGH", "RESISTENZA_UMANA", SeriesFormat.FACTION_SWAP,
                ChallengeStatus.PENDING, expiresAt);
    }

    @BeforeEach
    void wire() {
        when(challenges.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        when(views.refs(anyCollection())).thenReturn(Map.of(
                challengerId, new UserRef(challengerId, "luca"), opponentId, new UserRef(opponentId, "mattia")));
        Map<Faction, LockedDeck> decks = new EnumMap<>(Faction.class);
        for (Faction f : Faction.values()) {
            decks.put(f, new LockedDeck(f, UUID.randomUUID(), "[]"));
        }
        when(snapshots.lockChoices(any(), any())).thenReturn(decks);
        when(challengeDecks.findByChallengeId(any())).thenReturn(List.of());
    }

    private void lockReturns(Challenge c) {
        when(challenges.findByIdForUpdate(id)).thenReturn(Optional.of(c));
    }

    // -- accept ------------------------------------------------------------------------------------------

    @Test
    void acceptPlaysTheSeriesAndMarksTheChallengeAccepted() {
        Challenge c = pending(Instant.now().plus(Duration.ofHours(1)));
        lockReturns(c);
        MatchSeries series = mock(MatchSeries.class);
        when(runner.run(any(), any(), any(), any(), any(), anyMap(), anyMap(), any()))
                .thenReturn(new SeriesRunner.SeriesResult(series, List.of()));

        service.accept(opponentId, id, choices);

        assertEquals(ChallengeStatus.ACCEPTED, c.getStatus());
        assertNotNull(c.getResolvedAt());
        verify(runner).run(eq(SeriesKind.CHALLENGE), any(), eq(challengerId), eq(opponentId),
                eq(Faction.RESISTENZA_UMANA), anyMap(), anyMap(), eq(opponentId));
    }

    @Test
    void acceptingASecondTimeIsA409AndPlaysNothing() {
        Challenge c = pending(Instant.now().plus(Duration.ofHours(1)));
        c.setStatus(ChallengeStatus.ACCEPTED);
        lockReturns(c);

        ChallengeException e = assertThrows(ChallengeException.class, () -> service.accept(opponentId, id, choices));

        assertEquals(HttpStatus.CONFLICT, e.status());
        assertEquals(ErrorCodes.CHALLENGE_NOT_PENDING, e.code());
        verify(runner, never()).run(any(), any(), any(), any(), any(), anyMap(), anyMap(), any());
    }

    @Test
    void anExpiredChallengeIsMarkedExpiredAndRefused() {
        Challenge c = pending(Instant.now().minusSeconds(5));
        lockReturns(c);

        ChallengeException e = assertThrows(ChallengeException.class, () -> service.accept(opponentId, id, choices));

        assertEquals(ErrorCodes.CHALLENGE_EXPIRED, e.code());
        assertEquals(ChallengeStatus.EXPIRED, c.getStatus());
        verify(challenges).saveAndFlush(c); // written before the exception: the service does not roll it back
        verify(runner, never()).run(any(), any(), any(), any(), any(), anyMap(), anyMap(), any());
    }

    @Test
    void anEngineFaultLeavesTheChallengePending() {
        Challenge c = pending(Instant.now().plus(Duration.ofHours(1)));
        lockReturns(c);
        when(runner.run(any(), any(), any(), any(), any(), anyMap(), anyMap(), any()))
                .thenThrow(new EngineUnavailableException("down"));

        assertThrows(EngineUnavailableException.class, () -> service.accept(opponentId, id, choices));

        assertEquals(ChallengeStatus.PENDING, c.getStatus());
        verify(challenges, never()).saveAndFlush(any());
    }

    @Test
    void onlyTheChallengedPlayerCanAccept() {
        lockReturns(pending(Instant.now().plus(Duration.ofHours(1))));

        for (UUID other : List.of(challengerId, UUID.randomUUID())) {
            ChallengeException e = assertThrows(ChallengeException.class, () -> service.accept(other, id, choices));
            assertEquals(HttpStatus.NOT_FOUND, e.status());
            assertEquals(ErrorCodes.CHALLENGE_NOT_FOUND, e.code());
        }
        verify(runner, never()).run(any(), any(), any(), any(), any(), anyMap(), anyMap(), any());
    }

    @Test
    void aPlayerWhoCannotPlayCannotAccept() {
        lockReturns(pending(Instant.now().plus(Duration.ofHours(1))));
        org.mockito.Mockito.doThrow(new DeckException(HttpStatus.CONFLICT, ErrorCodes.DECK_DEFENSE_REQUIRED))
                .when(eligibility).requireCanPlay(opponentId);

        DeckException e = assertThrows(DeckException.class, () -> service.accept(opponentId, id, choices));

        assertEquals(ErrorCodes.DECK_DEFENSE_REQUIRED, e.code());
        verify(runner, never()).run(any(), any(), any(), any(), any(), anyMap(), anyMap(), any());
    }

    // -- decline and cancel ------------------------------------------------------------------------------

    @Test
    void theOpponentDeclinesAndTheChallengerCancels() {
        Challenge c = pending(Instant.now().plus(Duration.ofHours(1)));
        lockReturns(c);
        assertEquals(ChallengeStatus.DECLINED, service.decline(opponentId, id).status());

        Challenge d = pending(Instant.now().plus(Duration.ofHours(1)));
        lockReturns(d);
        assertEquals(ChallengeStatus.CANCELLED, service.cancel(challengerId, id).status());
    }

    @Test
    void theChallengerCannotDeclineAndTheOpponentCannotCancel() {
        lockReturns(pending(Instant.now().plus(Duration.ofHours(1))));

        assertEquals(ErrorCodes.CHALLENGE_NOT_FOUND,
                assertThrows(ChallengeException.class, () -> service.decline(challengerId, id)).code());
        assertEquals(ErrorCodes.CHALLENGE_NOT_FOUND,
                assertThrows(ChallengeException.class, () -> service.cancel(opponentId, id)).code());
    }

    // -- create and read ---------------------------------------------------------------------------------

    @Test
    void youCannotChallengeYourself() {
        User me = mock(User.class);
        when(me.getId()).thenReturn(challengerId);
        when(me.getRole()).thenReturn(Role.PLAYER);
        when(users.findByUsernameIgnoreCase("luca")).thenReturn(Optional.of(me));

        ChallengeException e = assertThrows(ChallengeException.class, () -> service.create(challengerId,
                new CreateChallengeRequest("luca", "RESISTENZA_UMANA", choices)));

        assertEquals(ErrorCodes.CHALLENGE_SELF, e.code());
    }

    @Test
    void anAdminOrAnUnknownNicknameIsNotFound() {
        User admin = mock(User.class);
        when(admin.getRole()).thenReturn(Role.ADMIN);
        when(users.findByUsernameIgnoreCase("root")).thenReturn(Optional.of(admin));

        for (String nick : List.of("root", "nobody")) {
            ChallengeException e = assertThrows(ChallengeException.class, () -> service.create(challengerId,
                    new CreateChallengeRequest(nick, "RESISTENZA_UMANA", choices)));
            assertEquals(ErrorCodes.CHALLENGE_OPPONENT_NOT_FOUND, e.code());
        }
    }

    @Test
    void aSecondPendingChallengeToTheSamePlayerIsRefused() {
        User opponent = mock(User.class);
        when(opponent.getId()).thenReturn(opponentId);
        when(opponent.getRole()).thenReturn(Role.PLAYER);
        when(users.findByUsernameIgnoreCase("Mattia")).thenReturn(Optional.of(opponent));
        when(challenges.existsByChallengerIdAndOpponentIdAndStatus(challengerId, opponentId, ChallengeStatus.PENDING))
                .thenReturn(true);

        ChallengeException e = assertThrows(ChallengeException.class, () -> service.create(challengerId,
                new CreateChallengeRequest("Mattia", "RESISTENZA_UMANA", choices)));

        assertEquals(ErrorCodes.CHALLENGE_ALREADY_PENDING, e.code());
        verify(challenges, never()).saveAndFlush(any());
    }

    @Test
    void aStrangerCannotReadAChallenge() {
        when(challenges.findById(id)).thenReturn(Optional.of(pending(Instant.now().plus(Duration.ofHours(1)))));

        ChallengeException e = assertThrows(ChallengeException.class, () -> service.get(UUID.randomUUID(), id));

        assertEquals(HttpStatus.NOT_FOUND, e.status());
    }

    @Test
    void aPendingChallengePastItsExpiryIsListedAsExpired() {
        when(challenges.findOfPlayer(challengerId)).thenReturn(List.of(pending(Instant.now().minusSeconds(60))));

        assertEquals(ChallengeStatus.EXPIRED, service.list(challengerId, Optional.empty()).get(0).status());
        assertEquals(1, service.list(challengerId, Optional.of(ChallengeStatus.EXPIRED)).size());
        assertEquals(0, service.list(challengerId, Optional.of(ChallengeStatus.PENDING)).size());
    }
}
