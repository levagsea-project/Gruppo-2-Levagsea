package quorma.backend.catalogue;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DraftItemRepository extends JpaRepository<DraftItem, UUID> {

    List<DraftItem> findByDraftId(UUID draftId);

    Optional<DraftItem> findByDraftIdAndItemTypeAndItemKey(UUID draftId, DraftItemType itemType, String itemKey);
}
