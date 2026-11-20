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

/** Name and description of a set in one language (V2 {@code card_set_texts}). */
@Entity
@Table(name = "card_set_texts")
@IdClass(CardSetText.Key.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CardSetText {

    public record Key(UUID setId, String locale) implements Serializable {
    }

    @Id
    private UUID setId;

    @Id
    private String locale;

    @Setter
    private String name;

    @Setter
    private String description;

    @UpdateTimestamp
    private Instant updatedAt;

    public CardSetText(UUID setId, String locale, String name, String description) {
        this.setId = setId;
        this.locale = locale;
        this.name = name;
        this.description = description;
    }
}
