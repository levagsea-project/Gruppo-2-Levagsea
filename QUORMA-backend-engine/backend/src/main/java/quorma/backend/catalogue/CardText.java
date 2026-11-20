package quorma.backend.catalogue;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

/** Name and flavour of a card in one language. Mutable and outside the snapshot hash (V2 {@code card_texts}). Italian is the fallback locale. */
@Entity
@Table(name = "card_texts")
@IdClass(CardText.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardText {

    public record Key(UUID cardId, String locale) implements Serializable {
    }

    @Id
    private UUID cardId;

    @Id
    private String locale;

    @Setter
    private String name;

    @Setter
    private String flavour;

    @UpdateTimestamp
    private Instant updatedAt;

    public CardText(UUID cardId, String locale, String name, String flavour) {
        this.cardId = cardId;
        this.locale = locale;
        this.name = name;
        this.flavour = flavour;
    }
}
