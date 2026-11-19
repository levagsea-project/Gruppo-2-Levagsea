package quorma.backend.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiveSnapshotRepository extends JpaRepository<ActiveSnapshot, String> {
}
