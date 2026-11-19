package quorma.backend.deck;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeckCardRepository extends JpaRepository<DeckCard, DeckCardId> {

    @Query("select c from DeckCard c where c.id.deckId = :deckId")
    List<DeckCard> findByDeckId(@Param("deckId") UUID deckId);

    @Query("select c from DeckCard c where c.id.deckId in :deckIds")
    List<DeckCard> findByDeckIds(@Param("deckIds") List<UUID> deckIds);

    /** Bulk delete: runs at once, so the same instances can be inserted again in the same transaction. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from DeckCard c where c.id.deckId = :deckId")
    void deleteByDeckId(@Param("deckId") UUID deckId);
}
