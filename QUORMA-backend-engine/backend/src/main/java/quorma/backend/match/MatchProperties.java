package quorma.backend.match;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code matches.*} (see application.yml). A challenge lasts at most 7 days, which is also how long a player can be
 * offline and still be picked by matchmaking.
 *
 * @param challengeTtl         how long a challenge by nickname waits for an answer
 * @param roomTtl              how long an open room waits for someone to join
 * @param matchmakingMaxOffline a player who has not logged in for longer is not picked as an opponent
 */
@ConfigurationProperties(prefix = "matches")
public record MatchProperties(Duration challengeTtl, Duration roomTtl, Duration matchmakingMaxOffline) {

    public static final Duration MAX_OFFLINE = Duration.ofDays(7);

    public MatchProperties {
        challengeTtl = cap(challengeTtl, Duration.ofHours(48));
        roomTtl = roomTtl == null ? Duration.ofMinutes(10) : roomTtl;
        matchmakingMaxOffline = cap(matchmakingMaxOffline, MAX_OFFLINE);
    }

    private static Duration cap(Duration value, Duration fallback) {
        if (value == null) {
            return fallback;
        }
        return value.compareTo(MAX_OFFLINE) > 0 ? MAX_OFFLINE : value;
    }
}
