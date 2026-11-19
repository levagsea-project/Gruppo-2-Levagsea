package quorma.backend.match;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import quorma.backend.catalogue.PoolSnapshotRepository;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;
import tools.jackson.databind.JsonNode;

/** Builds the API view of a series for one of its two players. Nothing here changes data. */
@Service
public class SeriesViewService {

    private final UserRepository users;
    private final PoolSnapshotRepository snapshots;
    private final ChallengeRepository challenges;
    private final MatchNotificationRepository notifications;

    public SeriesViewService(UserRepository users, PoolSnapshotRepository snapshots, ChallengeRepository challenges,
                             MatchNotificationRepository notifications) {
        this.users = users;
        this.snapshots = snapshots;
        this.challenges = challenges;
        this.notifications = notifications;
    }

    /**
     * @param logs the match logs by match id, when the caller has them (only the request that ran the series does);
     *             {@code null} for a history read, where the log is not stored
     */
    public SeriesResponse view(UUID viewerId, MatchSeries series, List<Match> games, Map<UUID, JsonNode> logs) {
        Map<UUID, UserRef> refs = refs(List.of(series.getPlayerAId(), series.getPlayerBId()));
        String code = series.getChallengeId() == null ? null
                : challenges.findById(series.getChallengeId()).map(Challenge::getCode).orElse(null);
        Map<UUID, String> factions = new HashMap<>();
        List<GameResponse> views = new ArrayList<>();
        for (Match m : games) {
            views.add(new GameResponse(m.getGameNumber(), m.getId(), faction(factions, m.getPoolSnapshotAId()),
                    faction(factions, m.getPoolSnapshotBId()), m.getOutcome(), m.getResultHash(), m.getPlayedAt(),
                    logs == null ? null : logs.get(m.getId())));
        }
        boolean unread = !games.isEmpty() && notifications
                .findOfPlayer(viewerId, games.stream().map(Match::getId).toList()).stream()
                .anyMatch(n -> n.getSeenAt() == null);
        return new SeriesResponse(series.getId(), series.getKind(), code, refs.get(series.getPlayerAId()),
                refs.get(series.getPlayerBId()), series.getPlayerAId().equals(viewerId) ? "A" : "B",
                series.getStatus(), series.getOutcome(), series.getFinishedAt(), unread, views);
    }

    /** Nickname and id of the given users. */
    public Map<UUID, UserRef> refs(Collection<UUID> ids) {
        return users.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, u -> new UserRef(u.getId(), u.getUsername())));
    }

    private String faction(Map<UUID, String> cache, UUID snapshotId) {
        return cache.computeIfAbsent(snapshotId, id -> snapshots.findById(id).orElseThrow().getFaction());
    }
}
