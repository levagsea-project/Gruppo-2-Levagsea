package quorma.backend.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {

    Optional<Card> findByCardCode(String cardCode);

    List<Card> findByCardCodeIn(Collection<String> cardCodes);
}
