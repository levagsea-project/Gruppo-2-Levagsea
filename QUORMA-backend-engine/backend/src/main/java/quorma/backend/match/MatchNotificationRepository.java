package quorma.backend.match;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchNotificationRepository extends JpaRepository<MatchNotification, MatchNotificationId> {
}
