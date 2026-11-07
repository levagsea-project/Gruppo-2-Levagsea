package quorma.backend.match;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {
}
