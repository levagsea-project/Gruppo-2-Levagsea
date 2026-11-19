package quorma.backend.match;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    List<Match> findBySeriesIdOrderByGameNumber(UUID seriesId);

    List<Match> findBySeriesIdInOrderBySeriesIdAscGameNumberAsc(Collection<UUID> seriesIds);
}
