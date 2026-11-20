package quorma.backend.catalogue;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardTextRepository extends JpaRepository<CardText, CardText.Key> {

    List<CardText> findByCardIdInAndLocaleIn(Collection<UUID> cardIds, Collection<String> locales);

    /** Cards of a language that already use this name, ignoring case (names are unique per language). */
    List<CardText> findByLocaleAndNameIgnoreCase(String locale, String name);
}
