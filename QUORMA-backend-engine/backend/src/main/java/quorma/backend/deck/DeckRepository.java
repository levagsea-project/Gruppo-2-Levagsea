package quorma.backend.deck;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckRepository extends JpaRepository<Deck, UUID> {

    List<Deck> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    Optional<Deck> findByIdAndOwnerId(UUID id, UUID ownerId);

    /** Returns true if the owner has a deck with the same name, excluding the given deck. For uniqueness. */
    boolean existsByOwnerIdAndNameAndIdNot(UUID ownerId, String name, UUID id);
}
