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
import org.hibernate.generator.EventType;

/** Stable identity of a card across pool versions. Stats and abilities live in {@link CardDefinition}. */
@Entity
@Table(name = "cards")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String cardCode;

    private String faction;

    @Enumerated(EnumType.STRING)
    private CardKind kind;

    private String subfaction;

    private String suit;

    private Short rank;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private Instant createdAt;

    @Generated(event = EventType.INSERT)
    @Column(insertable = false, updatable = false)
    private String deckSlot;

    public Card(String cardCode, String faction, CardKind kind, String subfaction, String suit, Short rank) {
        this.cardCode = cardCode;
        this.faction = faction;
        this.kind = kind;
        this.subfaction = subfaction;
        this.suit = suit;
        this.rank = rank;
    }
}
