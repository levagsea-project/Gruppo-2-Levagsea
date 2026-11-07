package quorma.backend.match;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** Per-player flag of a match: unread means seenAt is null. Created already seen when the player gets the result at once. */
@Entity
@Table(name = "match_notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MatchNotification {

    @EmbeddedId
    private MatchNotificationId id;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @Setter
    private Instant seenAt;

    public MatchNotification(MatchNotificationId id, Instant seenAt) {
        this.id = id;
        this.seenAt = seenAt;
    }
}
