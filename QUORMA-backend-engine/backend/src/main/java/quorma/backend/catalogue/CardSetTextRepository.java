package quorma.backend.catalogue;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardSetTextRepository extends JpaRepository<CardSetText, CardSetText.Key> {

    List<CardSetText> findBySetIdIn(Collection<UUID> setIds);
}
