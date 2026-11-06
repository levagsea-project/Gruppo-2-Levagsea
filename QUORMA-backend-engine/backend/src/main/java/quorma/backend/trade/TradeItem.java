package quorma.backend.trade;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One card inside a trade. */
@Entity
@Table(name = "trade_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TradeItem {

    @EmbeddedId
    private TradeItemId id;

    @Enumerated(EnumType.STRING)
    private TradeSide side;

    public TradeItem(TradeItemId id, TradeSide side) {
        this.id = id;
        this.side = side;
    }
}
