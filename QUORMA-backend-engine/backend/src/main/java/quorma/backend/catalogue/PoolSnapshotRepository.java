package quorma.backend.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface PoolSnapshotRepository extends JpaRepository<PoolSnapshot, UUID> {
}
