package quorma.backend.catalogue;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/** The one open draft of the publishing pipeline (V2 {@code catalogue_drafts}); status OPEN, PUBLISHED or DISCARDED, {@code source} seed, api or batch. */
@Entity
@Table(name = "catalogue_drafts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CatalogueDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Setter
    @Enumerated(EnumType.STRING)
    private DraftStatus status;

    private String source;

    @Setter
    private String note;

    private UUID createdBy;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @Setter
    private Instant closedAt;

    public CatalogueDraft(String source, String note, UUID createdBy) {
        this.status = DraftStatus.OPEN;
        this.source = source;
        this.note = note;
        this.createdBy = createdBy;
    }
}
