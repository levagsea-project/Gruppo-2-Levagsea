package quorma.backend.match;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.common.ErrorCodes;

/**
 * History of the series a player took part in, and delivery of results to the player who was not there when the
 * series was played (a challenge accepted later, an offline defender). Only the two players of a series can read it:
 * for anyone else it does not exist.
 */
@Service
public class MatchService {

    static final int MAX_PAGE_SIZE = 50;

    private final MatchSeriesRepository series;
    private final MatchRepository matches;
    private final MatchNotificationRepository notifications;
    private final SeriesViewService views;

    public MatchService(MatchSeriesRepository series, MatchRepository matches, MatchNotificationRepository notifications,
                        SeriesViewService views) {
        this.series = series;
        this.matches = matches;
        this.notifications = notifications;
        this.views = views;
    }

    /** My finished series, newest first, or only the ones whose result I have not read yet. */
    @Transactional(readOnly = true)
    public List<SeriesResponse> list(UUID userId, boolean unreadOnly, int page, int size) {
        List<MatchSeries> found = unreadOnly
                ? series.findUnreadOfPlayer(userId)
                : series.findFinishedOfPlayer(userId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE))).getContent();
        return render(userId, found);
    }

    /** One of my series; 404 if it does not exist or I did not play it. */
    @Transactional(readOnly = true)
    public SeriesResponse get(UUID userId, UUID seriesId) {
        return render(userId, List.of(mine(userId, seriesId))).get(0);
    }

    /** Marks the result of a series as read for me. */
    @Transactional
    public SeriesResponse markSeen(UUID userId, UUID seriesId) {
        MatchSeries s = mine(userId, seriesId);
        List<UUID> matchIds = matches.findBySeriesIdOrderByGameNumber(s.getId()).stream().map(Match::getId).toList();
        if (!matchIds.isEmpty()) {
            notifications.markSeen(userId, matchIds, Instant.now());
        }
        return render(userId, List.of(s)).get(0);
    }

    /** The series if it is finished and I played it; otherwise 404 (it does not exist for anyone else). */
    MatchSeries mine(UUID userId, UUID seriesId) {
        return series.findById(seriesId)
                .filter(s -> s.getStatus() == SeriesStatus.FINISHED)
                .filter(s -> s.getPlayerAId().equals(userId) || s.getPlayerBId().equals(userId))
                .orElseThrow(() -> new ChallengeException(HttpStatus.NOT_FOUND, ErrorCodes.MATCH_NOT_FOUND));
    }

    private List<SeriesResponse> render(UUID userId, List<MatchSeries> found) {
        if (found.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<Match>> games = matches
                .findBySeriesIdInOrderBySeriesIdAscGameNumberAsc(found.stream().map(MatchSeries::getId).toList()).stream()
                .collect(Collectors.groupingBy(Match::getSeriesId));
        return found.stream()
                .map(s -> views.view(userId, s, games.getOrDefault(s.getId(), List.of()), null))
                .toList();
    }
}
