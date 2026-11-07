package quorma.backend.trade;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeItemRepository extends JpaRepository<TradeItem, TradeItemId> {
}
