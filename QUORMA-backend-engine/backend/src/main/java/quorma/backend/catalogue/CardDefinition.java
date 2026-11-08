package quorma.backend.catalogue;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

/** Versioned content of a card inside one pool snapshot. Abilities (a JSON list, the same for every kind of card) are opaque to the backend and owned by the engine. */
@Entity
@Table(name = "card_definitions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID snapshotId;

    private UUID cardId;

    private String faction;

    @Enumerated(EnumType.STRING)
    private CardKind kind;

    private String name;

    private Integer atk;

    private Integer def;

    @JdbcTypeCode(SqlTypes.JSON)
    private String abilities;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    public CardDefinition(UUID snapshotId, UUID cardId, String faction, CardKind kind, String name, Integer atk, Integer def, String abilities) {
        this.snapshotId = snapshotId;
        this.cardId = cardId;
        this.faction = faction;
        this.kind = kind;
        this.name = name;
        this.atk = atk;
        this.def = def;
        this.abilities = abilities;
    }
}
