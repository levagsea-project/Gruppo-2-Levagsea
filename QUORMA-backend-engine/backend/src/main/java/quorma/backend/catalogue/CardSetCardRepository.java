package quorma.backend.catalogue;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardSetCardRepository extends JpaRepository<CardSetCard, CardSetCard.Key> {

    List<CardSetCard> findBySetId(UUID setId);

    List<CardSetCard> findByCardIdIn(Collection<UUID> cardIds);
}
