package quorma.backend.match;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchNotificationRepository extends JpaRepository<MatchNotification, MatchNotificationId> {

    /** The notification rows of a player for these matches. */
    @Query("select n from MatchNotification n where n.id.userId = :userId and n.id.matchId in :matchIds")
    List<MatchNotification> findOfPlayer(@Param("userId") UUID userId, @Param("matchIds") Collection<UUID> matchIds);

    /** Marks as read every unread notification of the player for these matches; returns how many. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update MatchNotification n set n.seenAt = :now where n.id.userId = :userId and n.seenAt is null"
            + " and n.id.matchId in :matchIds")
    int markSeen(@Param("userId") UUID userId, @Param("matchIds") Collection<UUID> matchIds, @Param("now") Instant now);
}
