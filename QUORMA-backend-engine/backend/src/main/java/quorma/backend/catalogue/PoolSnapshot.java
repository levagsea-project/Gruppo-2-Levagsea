package quorma.backend.catalogue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** Immutable, hashed snapshot of one faction's card pool. createdAt comes from the database clock on purpose (the sealing trigger compares it with now()). */
@Entity
@Table(name = "pool_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PoolSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String faction;

    private String versionLabel;

    private String rulesetVersion;

    private String contentHash;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    public PoolSnapshot(String faction, String versionLabel, String rulesetVersion, String contentHash) {
        this.faction = faction;
        this.versionLabel = versionLabel;
        this.rulesetVersion = rulesetVersion;
        this.contentHash = contentHash;
    }
}
