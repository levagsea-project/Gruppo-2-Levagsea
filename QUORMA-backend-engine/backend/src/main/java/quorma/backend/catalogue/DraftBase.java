package quorma.backend.catalogue;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** The snapshot a faction had active when the draft was opened (V2 {@code draft_bases}); null = none yet. Publishing fails if the active one has moved. */
@Entity
@Table(name = "draft_bases")
@IdClass(DraftBase.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DraftBase {

    public record Key(UUID draftId, String faction) implements Serializable {
    }

    @Id
    private UUID draftId;

    @Id
    private String faction;

    private UUID snapshotId;

    public DraftBase(UUID draftId, String faction, UUID snapshotId) {
        this.draftId = draftId;
        this.faction = faction;
        this.snapshotId = snapshotId;
    }
}
