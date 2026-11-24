package quorma.backend.telemetry;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.ActiveSnapshotService;
import quorma.backend.catalogue.Card;
import quorma.backend.catalogue.CardPresenter;
import quorma.backend.catalogue.CardRepository;
import quorma.backend.catalogue.Faction;
import quorma.backend.catalogue.PreferredLocales;
import quorma.backend.common.ErrorCodes;
import quorma.backend.user.Role;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;

/** Reads statistics from the aggregation tables. Reading never waits for the background service. */
@Service
public class StatsService {

    private final UserStatsRepository userStats;
    private final UserHandStatsRepository handStats;
    private final UserFactionStatsRepository factionStats;
    private final UserCardStatsRepository userCardStats;
    private final CardStatsRepository cardStats;
    private final UserRepository users;
    private final CardRepository cards;
    private final CardPresenter presenter;
    private final ActiveSnapshotService activeSnapshots;

    public StatsService(UserStatsRepository userStats, UserHandStatsRepository handStats,
                        UserFactionStatsRepository factionStats, UserCardStatsRepository userCardStats,
                        CardStatsRepository cardStats, UserRepository users, CardRepository cards,
                        CardPresenter presenter, ActiveSnapshotService activeSnapshots) {
        this.userStats = userStats;
        this.handStats = handStats;
        this.factionStats = factionStats;
        this.userCardStats = userCardStats;
        this.cardStats = cardStats;
        this.users = users;
        this.cards = cards;
        this.presenter = presenter;
        this.activeSnapshots = activeSnapshots;
    }

    @Transactional(readOnly = true)
    public StatsResponse forUser(UUID userId) {
        UserStats s = userStats.findById(userId).orElse(null);
        var hands = handStats.findByIdUserId(userId).stream()
                .filter(h -> h.getTurns() > 0)
                .map(h -> new StatsResponse.HandCount(h.getId().handType(), h.getTurns()))
                .sorted(Comparator.comparingLong(StatsResponse.HandCount::turns).reversed()
                        .thenComparing(StatsResponse.HandCount::hand))
                .toList();
        var factions = factionStats.findByIdUserId(userId).stream()
                .map(f -> new StatsResponse.FactionRecord(f.getId().faction(), f.getGamesPlayed(), f.getGamesWon()))
                .sorted(Comparator.comparing(StatsResponse.FactionRecord::faction))
                .toList();
        if (s == null) {
            return new StatsResponse(0, 0, 0, 0, 0, 0, 0, 0, 0, hands, factions);
        }
        return new StatsResponse(s.getSeriesPlayed(), s.getSeriesWins(), s.getSeriesLosses(), s.getSeriesDraws(),
                s.getMatchesPlayed(), s.getWins(), s.getDamageDealt(), s.getSpillDamage(), s.getKills(), hands, factions);
    }

    /** Another player's statistics, by username; 404 {@code user.not_found} if there is no such player (admins do not play). */
    @Transactional(readOnly = true)
    public StatsResponse forUsername(String username) {
        User user = users.findByUsernameIgnoreCase(username)
                .filter(u -> u.getRole() == Role.PLAYER)
                .orElseThrow(() -> new StatsException(HttpStatus.NOT_FOUND, ErrorCodes.USER_NOT_FOUND));
        return forUser(user.getId());
    }

    /** The player's own figures for each card they played, the most played first. */
    @Transactional(readOnly = true)
    public List<CardFiguresResponse> cardsOfUser(UUID userId, PreferredLocales locales) {
        return figures(userCardStats.findByIdUserId(userId).stream()
                .map(c -> new Row(c.getId().cardId(), c.getMatchesPlayed(), c.getMatchesWon(), c.getTimesDrawn(),
                        c.getDamageDealt(), c.getSpillDamage(), c.getKills()))
                .toList(), locales);
    }

    /**
     * All players' figures for each card of one pool snapshot (balance tuning): {@code snapshotId} if given, else the
     * active snapshot of the faction. A faction with no active snapshot has none.
     */
    @Transactional(readOnly = true)
    public List<CardFiguresResponse> cardsOfSnapshot(Faction faction, UUID snapshotId, PreferredLocales locales) {
        UUID snapshot = snapshotId != null ? snapshotId : activeSnapshots.activeId(faction.name()).orElse(null);
        if (snapshot == null) {
            return List.of();
        }
        return figures(cardStats.findByIdSnapshotId(snapshot).stream()
                .map(c -> new Row(c.getId().cardId(), c.getMatchesPlayed(), c.getMatchesWon(), c.getTimesDrawn(),
                        c.getDamageDealt(), c.getSpillDamage(), c.getKills()))
                .toList(), locales);
    }

    private record Row(UUID cardId, long played, long won, long drawn, long damage, long spill, long kills) {
    }

    private List<CardFiguresResponse> figures(List<Row> rows, PreferredLocales locales) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = rows.stream().map(Row::cardId).toList();
        Map<UUID, Card> cardById = cards.findAllById(ids).stream().collect(Collectors.toMap(Card::getId, Function.identity()));
        Map<UUID, CardPresenter.CardFace> faces = presenter.faces(ids, locales);
        return rows.stream()
                .map(r -> {
                    CardPresenter.CardFace face = faces.get(r.cardId());
                    return new CardFiguresResponse(r.cardId(), cardById.get(r.cardId()).getCardCode(),
                            face == null ? null : face.name(), face == null ? null : face.textLocale(),
                            r.played(), r.won(), r.drawn(), r.damage(), r.spill(), r.kills());
                })
                .sorted(Comparator.comparingLong(CardFiguresResponse::matchesPlayed).reversed()
                        .thenComparing(CardFiguresResponse::cardCode))
                .toList();
    }
}
