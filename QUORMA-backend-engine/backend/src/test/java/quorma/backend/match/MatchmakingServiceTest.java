package quorma.backend.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;
import quorma.backend.deck.DeckEligibility;
import quorma.backend.deck.DeckException;
import quorma.backend.user.UserRepository;

/** Matchmaking: who may be picked, and that nothing is played when nobody can. */
class MatchmakingServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final DeckEligibility eligibility = mock(DeckEligibility.class);
    private final DeckSnapshotService snapshots = mock(DeckSnapshotService.class);
    private final SeriesRunner runner = mock(SeriesRunner.class);
    private final SeriesViewService views = mock(SeriesViewService.class);
    private final MatchProperties props = new MatchProperties(null, null, null);
    private final MatchmakingService service = new MatchmakingService(users, eligibility, snapshots, runner, views, props);

    private final UUID me = UUID.randomUUID();
    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final MatchmakingRequest request = new MatchmakingRequest("POTENZE_ARTIFICIALI", List.of(
            new DeckChoice("RESISTENZA_UMANA", UUID.randomUUID()), new DeckChoice("POTENZE_ARTIFICIALI", UUID.randomUUID())));

    private static DeckEligibility.PlayStatus status(boolean canPlay) {
        return new DeckEligibility.PlayStatus(canPlay, List.of(), List.of(), List.of());
    }

    @BeforeEach
    void wire() {
        Map<Faction, LockedDeck> decks = new EnumMap<>(Faction.class);
        for (Faction f : Faction.values()) {
            decks.put(f, new LockedDeck(f, UUID.randomUUID(), "[]"));
        }
        when(snapshots.lockChoices(any(), any())).thenReturn(decks);
        when(snapshots.lockDefense(any())).thenReturn(decks);
        when(runner.run(any(), any(), any(), any(), any(), anyMap(), anyMap(), any()))
                .thenReturn(new SeriesRunner.SeriesResult(mock(MatchSeries.class), List.of()));
    }

    @Test
    void playsAgainstTheFirstCandidateWhoCanPlayAndAsksForRecentPlayersOnly() {
        when(users.findRecentPlayers(eq(me), any(), anyInt())).thenReturn(List.of(first, second));
        when(eligibility.status(first)).thenReturn(status(false)); // an outdated defense deck: skipped
        when(eligibility.status(second)).thenReturn(status(true));

        service.play(me, request);

        verify(runner).run(eq(SeriesKind.MATCHMAKING), eq(null), eq(me), eq(second), eq(Faction.POTENZE_ARTIFICIALI),
                anyMap(), anyMap(), eq(me));
        ArgumentCaptor<Instant> since = ArgumentCaptor.forClass(Instant.class);
        verify(users).findRecentPlayers(eq(me), since.capture(), eq(MatchmakingService.CANDIDATES));
        Duration offline = Duration.between(since.getValue(), Instant.now());
        assertTrue(offline.compareTo(Duration.ofDays(7).plusMinutes(1)) < 0, "at most 7 days offline");
        assertTrue(offline.compareTo(Duration.ofDays(7).minusMinutes(1)) > 0);
    }

    @Test
    void theOfflineLimitNeverGoesPastSevenDays() {
        MatchProperties longer = new MatchProperties(null, null, Duration.ofDays(30));
        assertEquals(Duration.ofDays(7), longer.matchmakingMaxOffline());
        assertEquals(Duration.ofDays(7), new MatchProperties(Duration.ofDays(30), null, null).challengeTtl());
    }

    @Test
    void aCandidateWhoseDefenseDecksCannotBeUsedIsSkipped() {
        when(users.findRecentPlayers(eq(me), any(), anyInt())).thenReturn(List.of(first, second));
        when(eligibility.status(any())).thenReturn(status(true));
        Map<Faction, LockedDeck> decks = new EnumMap<>(Faction.class);
        for (Faction f : Faction.values()) {
            decks.put(f, new LockedDeck(f, UUID.randomUUID(), "[]"));
        }
        when(snapshots.lockDefense(first)).thenThrow(new DeckException(HttpStatus.CONFLICT, ErrorCodes.DECK_OUTDATED));
        when(snapshots.lockDefense(second)).thenReturn(decks);

        service.play(me, request);

        verify(runner).run(any(), any(), eq(me), eq(second), any(), anyMap(), anyMap(), any());
    }

    @Test
    void withNobodyAvailableNothingIsPlayed() {
        when(users.findRecentPlayers(eq(me), any(), anyInt())).thenReturn(List.of());

        ChallengeException e = assertThrows(ChallengeException.class, () -> service.play(me, request));

        assertEquals(HttpStatus.NOT_FOUND, e.status());
        assertEquals(ErrorCodes.MATCHMAKING_NO_OPPONENT, e.code());
        verify(runner, never()).run(any(), any(), any(), any(), any(), anyMap(), anyMap(), any());
    }

    @Test
    void whenEveryCandidateIsBlockedThereIsNoOpponent() {
        when(users.findRecentPlayers(eq(me), any(), anyInt())).thenReturn(List.of(first, second));
        when(eligibility.status(any())).thenReturn(status(false));

        assertEquals(ErrorCodes.MATCHMAKING_NO_OPPONENT,
                assertThrows(ChallengeException.class, () -> service.play(me, request)).code());
    }

    @Test
    void aPlayerWhoCannotPlayCannotLookForAnOpponent() {
        org.mockito.Mockito.doThrow(new DeckException(HttpStatus.CONFLICT, ErrorCodes.DECK_DEFENSE_REQUIRED))
                .when(eligibility).requireCanPlay(me);

        assertThrows(DeckException.class, () -> service.play(me, request));
        verify(users, never()).findRecentPlayers(any(), any(), anyInt());
    }
}
