package quorma.backend.catalogue;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Which snapshot of a faction is the active one (V2 {@code active_snapshots}). Not the newest: publishing may re-activate an old snapshot. */
@Entity
@Table(name = "active_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActiveSnapshot {

    @Id
    private String faction;

    @Setter
    private UUID snapshotId;

    @Setter
    private Instant activatedAt;

    public ActiveSnapshot(String faction, UUID snapshotId, Instant activatedAt) {
        this.faction = faction;
        this.snapshotId = snapshotId;
        this.activatedAt = activatedAt;
    }
}
