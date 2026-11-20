package quorma.backend.catalogue;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

/** One change inside a draft (V2 {@code draft_items}): item_type CARD, TEXT or SET, identified by item_key. The payload is JSON the backend stores and passes on. */
@Entity
@Table(name = "draft_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DraftItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID draftId;

    @Enumerated(EnumType.STRING)
    private DraftItemType itemType;

    private String itemKey;

    @Setter
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    public DraftItem(UUID draftId, DraftItemType itemType, String itemKey, String payload) {
        this.draftId = draftId;
        this.itemType = itemType;
        this.itemKey = itemKey;
        this.payload = payload;
    }
}
