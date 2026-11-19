package quorma.backend.catalogue;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DraftBaseRepository extends JpaRepository<DraftBase, DraftBase.Key> {

    List<DraftBase> findByDraftId(UUID draftId);
}
