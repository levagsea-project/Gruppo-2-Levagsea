package quorma.backend.deck;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckRepository extends JpaRepository<Deck, UUID> {

    List<Deck> findByOwnerIdOrderByUpdatedAtDesc(UUID ownerId);

    /** The defense decks of a player: at most one per faction. */
    List<Deck> findByOwnerIdAndDefenseTrue(UUID ownerId);

    /** Decks of a faction that are not on the given snapshot: the ones a new snapshot may take over. */
    List<Deck> findByFactionAndSnapshotIdNot(String faction, UUID snapshotId);

    Optional<Deck> findByIdAndOwnerId(UUID id, UUID ownerId);

    /** Returns true if the owner has a deck with the same name, excluding the given deck. For uniqueness. */
    boolean existsByOwnerIdAndNameAndIdNot(UUID ownerId, String name, UUID id);
}
