package quorma.backend.collection;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CardInstanceRepository extends JpaRepository<CardInstance, UUID> {

    List<CardInstance> findByOwnerIdOrderByAcquiredAtDesc(UUID ownerId);

    /** The instances created by one pack opening, grant, etc., whoever owns them now. */
    List<CardInstance> findByOriginRef(UUID originRef);
}
