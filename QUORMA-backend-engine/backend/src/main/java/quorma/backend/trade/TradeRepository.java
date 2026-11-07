package quorma.backend.trade;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface TradeRepository extends JpaRepository<Trade, UUID> {
}
