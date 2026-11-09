package quorma.gameengine.model;

import java.util.List;
import java.util.Objects;
import quorma.gameengine.hand.Suit;

/**
 * A creature card as printed: identity, base stats and abilities. The runtime state of a creature in a lane is
 * {@link Creature}.
 *
 * @param rank      1..13, Ace = 1, Jack = 11, Queen = 12, King = 13
 * @param atk       base attack, 0 or more
 * @param def       base defence, 1 or more
 * @param abilities keywords, an evolution, or whole effects, in card order (see {@link Ability})
 */
public record CreatureCard(
        String id, int rank, Suit suit, Subfaction subfaction, int atk, int def, List<Ability> abilities)
        implements Card {

    public CreatureCard {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(suit, "suit");
        Objects.requireNonNull(subfaction, "subfaction");
        if (rank < 1 || rank > 13) {
            throw new IllegalArgumentException("rank must be 1..13, got " + rank);
        }
        if (atk < 0) {
            throw new IllegalArgumentException("atk must be 0 or more, got " + atk);
        }
        if (def < 1) {
            throw new IllegalArgumentException("def must be 1 or more, got " + def);
        }
        abilities = List.copyOf(abilities);
    }

    /** A creature with no abilities. */
    public CreatureCard(String id, int rank, Suit suit, Subfaction subfaction, int atk, int def) {
        this(id, rank, suit, subfaction, atk, def, List.of());
    }

    @Override
    public Faction faction() {
        return subfaction.faction();
    }
}
