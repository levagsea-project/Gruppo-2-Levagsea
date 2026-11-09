package quorma.backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {
}
