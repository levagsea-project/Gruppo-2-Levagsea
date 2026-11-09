package quorma.backend.deck;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DeckCardRepository extends JpaRepository<DeckCard, DeckCardId> {
}
