package quorma.backend.catalogue;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapshotPublicationRepository extends JpaRepository<SnapshotPublication, UUID> {

    List<SnapshotPublication> findBySnapshotIdOrderByPublishedAtDesc(UUID snapshotId);
}
