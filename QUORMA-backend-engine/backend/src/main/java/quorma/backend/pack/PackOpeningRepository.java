package quorma.backend.pack;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PackOpeningRepository extends JpaRepository<PackOpening, UUID> {

    /** Always look a key up together with the user from the JWT: a key means nothing for another user. */
    Optional<PackOpening> findByUserIdAndIdempotencyKey(UUID userId, UUID idempotencyKey);
}
