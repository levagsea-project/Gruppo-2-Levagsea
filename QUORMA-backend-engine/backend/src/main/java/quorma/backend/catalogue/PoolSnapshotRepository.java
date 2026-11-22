package quorma.backend.catalogue;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoolSnapshotRepository extends JpaRepository<PoolSnapshot, UUID> {

    /** The snapshot with this content hash, if one was ever published (the hash is unique). */
    Optional<PoolSnapshot> findByContentHash(String contentHash);
}
