package quorma.backend.catalogue;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** Audit row of one publish of one faction (V2 {@code snapshot_publications}, append-only). {@code reactivated}: the candidate's hash already existed and that snapshot became active again. */
@Entity
@Table(name = "snapshot_publications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SnapshotPublication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID snapshotId;

    private UUID draftId;

    private UUID basedOnId;

    private String source;

    private boolean reactivated;

    private UUID publishedBy;

    private String note;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant publishedAt;

    public SnapshotPublication(UUID snapshotId, UUID draftId, UUID basedOnId, String source, boolean reactivated,
                               UUID publishedBy, String note) {
        this.snapshotId = snapshotId;
        this.draftId = draftId;
        this.basedOnId = basedOnId;
        this.source = source;
        this.reactivated = reactivated;
        this.publishedBy = publishedBy;
        this.note = note;
    }
}
