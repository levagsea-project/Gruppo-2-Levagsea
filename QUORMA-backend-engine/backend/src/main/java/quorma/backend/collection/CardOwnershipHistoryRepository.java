package quorma.backend.collection;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CardOwnershipHistoryRepository extends JpaRepository<CardOwnershipHistory, UUID> {
}
