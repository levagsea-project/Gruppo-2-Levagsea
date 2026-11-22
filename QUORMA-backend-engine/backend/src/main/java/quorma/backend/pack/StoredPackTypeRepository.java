package quorma.backend.pack;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredPackTypeRepository extends JpaRepository<StoredPackType, String> {

    List<StoredPackType> findBySetId(UUID setId);
}
