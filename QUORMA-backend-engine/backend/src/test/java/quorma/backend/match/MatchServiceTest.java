package quorma.backend.match;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import quorma.backend.common.ErrorCodes;

/** History and delivery of results: only the two players see a series, and unread results go to the one who was away. */
class MatchServiceTest {

    private final MatchSeriesRepository series = mock(MatchSeriesRepository.class);
    private final MatchRepository matches = mock(MatchRepository.class);
    private final MatchNotificationRepository notifications = mock(MatchNotificationRepository.class);
    private final SeriesViewService views = mock(SeriesViewService.class);
    private final MatchService service = new MatchService(series, matches, notifications, views);

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID seriesId = UUID.randomUUID();

    private MatchSeries finished() {
        MatchSeries s = mock(MatchSeries.class);
        when(s.getId()).thenReturn(seriesId);
        when(s.getPlayerAId()).thenReturn(a);
        when(s.getPlayerBId()).thenReturn(b);
        when(s.getStatus()).thenReturn(SeriesStatus.FINISHED);
        when(series.findById(seriesId)).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    void bothPlayersCanReadASeriesAndAStrangerCannot() {
        MatchSeries s = finished();
        when(views.view(any(), eq(s), any(), eq(null))).thenReturn(mock(SeriesResponse.class));

        service.get(a, seriesId);
        service.get(b, seriesId);

        ChallengeException e = assertThrows(ChallengeException.class, () -> service.get(UUID.randomUUID(), seriesId));
        assertEquals(HttpStatus.NOT_FOUND, e.status());
        assertEquals(ErrorCodes.MATCH_NOT_FOUND, e.code());
    }

    @Test
    void aSeriesStillInProgressIsNotShown() {
        MatchSeries s = mock(MatchSeries.class);
        when(s.getStatus()).thenReturn(SeriesStatus.IN_PROGRESS);
        when(series.findById(seriesId)).thenReturn(Optional.of(s));

        assertThrows(ChallengeException.class, () -> service.get(a, seriesId));
    }

    @Test
    void theHistoryIsPagedWithACap() {
        when(series.findFinishedOfPlayer(eq(a), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        assertTrue(service.list(a, false, -3, 5000).isEmpty());

        org.mockito.ArgumentCaptor<Pageable> page = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(series).findFinishedOfPlayer(eq(a), page.capture());
        assertEquals(0, page.getValue().getPageNumber());
        assertEquals(MatchService.MAX_PAGE_SIZE, page.getValue().getPageSize());
    }

    @Test
    void unreadOnlyAsksForTheUnreadSeries() {
        when(series.findUnreadOfPlayer(b)).thenReturn(List.of());

        assertTrue(service.list(b, true, 0, 20).isEmpty());

        verify(series).findUnreadOfPlayer(b);
        verify(series, never()).findFinishedOfPlayer(any(), any());
    }

    @Test
    void markingSeenTouchesOnlyTheNotificationsOfThatPlayer() {
        finished();
        Match game = mock(Match.class);
        UUID matchId = UUID.randomUUID();
        when(game.getId()).thenReturn(matchId);
        when(matches.findBySeriesIdOrderByGameNumber(seriesId)).thenReturn(List.of(game));
        when(matches.findBySeriesIdInOrderBySeriesIdAscGameNumberAsc(anyCollection())).thenReturn(List.of(game));
        when(game.getSeriesId()).thenReturn(seriesId);

        service.markSeen(b, seriesId);

        verify(notifications).markSeen(eq(b), eq(List.of(matchId)), any());
        assertThrows(ChallengeException.class, () -> service.markSeen(UUID.randomUUID(), seriesId));
    }
}
