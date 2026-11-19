package quorma.backend.deck;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.catalogue.Faction;
import quorma.backend.common.ErrorCodes;

/**
 * Which decks a player may use (2026-11-17-deck-migration). An outdated deck cannot be selected, and a player must
 * have a legal defense deck for both factions to play: challenge, accept a challenge or matchmaking. The code that
 * starts a challenge or a series calls {@link #requireCanPlay}, and {@link #requireSelectable} for every deck it takes.
 */
@Service
public class DeckEligibility {

    private final DeckRepository decks;
    private final DeckMigrator migrator;

    public DeckEligibility(DeckRepository decks, DeckMigrator migrator) {
        this.decks = decks;
        this.migrator = migrator;
    }

    /** An outdated deck, for the message the frontend shows when the home page opens. */
    public record OutdatedDeck(UUID id, String name, String faction, boolean defense) {}

    /**
     * @param outdatedDefense factions whose defense deck is outdated
     * @param missingDefense  factions with no defense deck at all
     * @param outdatedDecks   every outdated deck of the player, defense or not
     */
    public record PlayStatus(boolean canPlay, List<String> outdatedDefense, List<String> missingDefense,
                             List<OutdatedDeck> outdatedDecks) {}

    @Transactional(readOnly = true)
    public PlayStatus status(UUID userId) {
        List<Deck> defense = decks.findByOwnerIdAndDefenseTrue(userId);
        List<String> outdated = Arrays.stream(Faction.values()).map(f -> f.name())
                .filter(f -> defense.stream().anyMatch(d -> d.getFaction().equals(f) && d.isOutdated())).toList();
        List<String> missing = Arrays.stream(Faction.values()).map(f -> f.name())
                .filter(f -> defense.stream().noneMatch(d -> d.getFaction().equals(f))).toList();
        List<OutdatedDeck> outdatedDecks = decks.findByOwnerIdOrderByUpdatedAtDesc(userId).stream()
                .filter(d -> d.isOutdated())
                .map(d -> new OutdatedDeck(d.getId(), d.getName(), d.getFaction(), d.isDefense()))
                .toList();
        return new PlayStatus(outdated.isEmpty() && missing.isEmpty(), outdated, missing, outdatedDecks);
    }

    /**
     * 409 {@code deck.defense_required} unless the player has a legal defense deck for both factions. First moves the
     * player's decks to the active snapshot ({@link DeckMigrator}), a safety net for a session that outlives a publish:
     * with every deck already on it this does nothing, and a failure is logged without blocking.
     */
    @Transactional(readOnly = true)
    public void requireCanPlay(UUID userId) {
        migrator.migrateOwnerQuietly(userId);
        if (!status(userId).canPlay()) {
            throw new DeckException(HttpStatus.CONFLICT, ErrorCodes.DECK_DEFENSE_REQUIRED);
        }
    }

    /** 409 {@code deck.outdated} for a deck that cannot be selected. */
    public void requireSelectable(Deck deck) {
        if (deck.isOutdated()) {
            throw new DeckException(HttpStatus.CONFLICT, ErrorCodes.DECK_OUTDATED);
        }
    }
}
