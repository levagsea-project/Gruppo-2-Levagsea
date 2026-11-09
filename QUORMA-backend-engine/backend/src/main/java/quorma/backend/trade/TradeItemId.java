package quorma.backend.trade;

import jakarta.persistence.Embeddable;
import java.util.UUID;

@Embeddable
public record TradeItemId(UUID tradeId, UUID cardInstanceId) {
}
