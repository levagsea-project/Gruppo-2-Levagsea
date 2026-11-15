package quorma.backend.catalogue;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CardDefinitionRepository extends JpaRepository<CardDefinition, UUID> {

    List<CardDefinition> findBySnapshotId(UUID snapshotId);

    List<CardDefinition> findBySnapshotIdAndCardIdIn(UUID snapshotId, Collection<UUID> cardIds);
}
