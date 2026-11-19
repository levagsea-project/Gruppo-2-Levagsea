package quorma.backend.pack;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.generator.EventType;

/**
 * A pack type as stored in the database (V8 {@code pack_types}): one set, a price, the slots per Tier and the
 * support slots. Mutable, like the set. The set of a type never changes once packs were sold (a foreign
 * key from {@code pack_openings} enforces it): create a new type instead. Until the shop reads this table the
 * {@link PackType} record from {@code application.yml} still describes the pack.
 */
@Entity
@Table(name = "pack_types")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredPackType {

    @Id
    private String code;

    private UUID setId;

    @Setter
    private long price;

    @Setter
    @Column(name = "slots_tier_1")
    private short slotsTier1;

    @Setter
    @Column(name = "slots_tier_2")
    private short slotsTier2;

    @Setter
    @Column(name = "slots_tier_3")
    private short slotsTier3;

    @Setter
    @Column(name = "slots_tier_4")
    private short slotsTier4;

    @Setter
    private short supportSlots;

    @Setter
    private boolean featured;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public StoredPackType(String code, UUID setId, long price, short slotsTier1, short slotsTier2,
                          short slotsTier3, short slotsTier4, short supportSlots, boolean featured) {
        this.code = code;
        this.setId = setId;
        this.price = price;
        this.slotsTier1 = slotsTier1;
        this.slotsTier2 = slotsTier2;
        this.slotsTier3 = slotsTier3;
        this.slotsTier4 = slotsTier4;
        this.supportSlots = supportSlots;
        this.featured = featured;
    }
}
