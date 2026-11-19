package quorma.backend.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PoolSnapshotRepository extends JpaRepository<PoolSnapshot, UUID> {

    /** The most recent snapshot of a faction, if any. */
    Optional<PoolSnapshot> findFirstByFactionOrderByCreatedAtDesc(String faction);
}
