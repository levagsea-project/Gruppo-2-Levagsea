package quorma.backend.catalogue;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Which snapshot of a faction is the active one. Always read it here (the {@code active_snapshots} row), never as "the
 * newest by {@code created_at}": publishing may re-activate an older snapshot.
 */
@Service
public class ActiveSnapshotService {

    private final ActiveSnapshotRepository active;

    public ActiveSnapshotService(ActiveSnapshotRepository active) {
        this.active = active;
    }

    /** The id of the active snapshot of {@code faction}, if one was ever published. */
    @Transactional(readOnly = true)
    public Optional<UUID> activeId(String faction) {
        return active.findById(faction).map(ActiveSnapshot::getSnapshotId);
    }

    /** True if {@code snapshotId} is the active snapshot of {@code faction}. */
    @Transactional(readOnly = true)
    public boolean isActive(String faction, UUID snapshotId) {
        return activeId(faction).filter(snapshotId::equals).isPresent();
    }
}
