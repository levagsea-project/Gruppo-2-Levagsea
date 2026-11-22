package quorma.backend.catalogue;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardSetRepository extends JpaRepository<CardSet, UUID> {

    Optional<CardSet> findByCode(String code);
}
