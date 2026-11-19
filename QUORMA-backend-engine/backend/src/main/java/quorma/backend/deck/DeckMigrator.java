package quorma.backend.deck;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import quorma.backend.catalogue.ActiveSnapshotService;
import quorma.backend.catalogue.Faction;

/**
 * Moves a player's decks to the active snapshot of their faction, when the player logs in
 * (2026-11-17-deck-migration, refined by docs/plans/cards-into-game.md). Publishing never touches a deck: a deck is
 * pinned to a snapshot and each side of a match stores its own, so decks on different snapshots can play each other.
 *
 * <p>For each deck of the owner that is not on the active snapshot of its faction (or is flagged outdated), the deck is
 * reloaded against the active snapshot and checked by {@link DeckValidator}: if legal it moves there and is no longer
 * outdated, if not it is flagged {@code outdated}. "Outdated" means one thing: the deck is illegal on the active
 * snapshot. With every deck already on the active snapshot this does nothing.
 *
 * <p>Each deck is migrated in its own transaction, separate from the caller's: a failure on one deck is logged by
 * {@link #migrateOwnerQuietly} and leaves that deck on its old snapshot, playable, without blocking the login.
 */
@Service
public class DeckMigrator {

    private static final Logger LOG = LoggerFactory.getLogger(DeckMigrator.class);

    private final DeckRepository decks;
    private final DeckCardRepository deckCards;
    private final DeckLoader loader;
    private final DeckValidator validator;
    private final ActiveSnapshotService activeSnapshots;
    private final TransactionTemplate tx;

    public DeckMigrator(DeckRepository decks, DeckCardRepository deckCards, DeckLoader loader, DeckValidator validator,
                        ActiveSnapshotService activeSnapshots, PlatformTransactionManager txManager) {
        this.decks = decks;
        this.deckCards = deckCards;
        this.loader = loader;
        this.validator = validator;
        this.activeSnapshots = activeSnapshots;
        this.tx = new TransactionTemplate(txManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** What happened to the decks of one owner. */
    public record Result(int moved, int flagged, int unchanged) {
    }

    private enum Outcome { MOVED, FLAGGED, UNCHANGED }

    /** Migrates the decks of {@code ownerId}; the first failure is thrown (decks already done stay done). */
    public Result migrateOwner(UUID ownerId) {
        return migrate(ownerId, true);
    }

    /** Same as {@link #migrateOwner}, but a failure is logged and the other decks are still tried. */
    public void migrateOwnerQuietly(UUID ownerId) {
        try {
            migrate(ownerId, false);
        } catch (RuntimeException e) {
            LOG.warn("deck migration of user {} failed, their decks stay as they are", ownerId, e);
        }
    }

    private Result migrate(UUID ownerId, boolean strict) {
        int moved = 0;
        int flagged = 0;
        int unchanged = 0;
        List<UUID> deckIds = decks.findByOwnerIdOrderByUpdatedAtDesc(ownerId).stream().map(Deck::getId).toList();
        for (UUID deckId : deckIds) {
            Outcome outcome;
            try {
                outcome = tx.execute(status -> migrateDeck(deckId));
            } catch (RuntimeException e) {
                if (strict) {
                    throw e;
                }
                LOG.warn("deck {} of user {} could not be migrated, it stays as it is", deckId, ownerId, e);
                continue;
            }
            switch (outcome) {
                case MOVED -> moved++;
                case FLAGGED -> flagged++;
                case UNCHANGED -> unchanged++;
            }
        }
        return new Result(moved, flagged, unchanged);
    }

    private Outcome migrateDeck(UUID deckId) {
        Deck deck = decks.findById(deckId).orElse(null);
        if (deck == null) {
            return Outcome.UNCHANGED;
        }
        UUID activeId = activeSnapshots.activeId(deck.getFaction()).orElse(null);
        if (activeId == null || (activeId.equals(deck.getSnapshotId()) && !deck.isOutdated())) {
            return Outcome.UNCHANGED;
        }
        List<UUID> instanceIds = deckCards.findByDeckId(deckId).stream().map(dc -> dc.getId().cardInstanceId()).toList();
        List<DeckError> errors = new ArrayList<>();
        List<DeckCardResponse> views = loader.load(deck.getOwnerId(), deck.getFaction(), activeId, instanceIds, errors);
        errors.addAll(validator.validate(Faction.valueOf(deck.getFaction()), views));
        if (errors.isEmpty()) {
            deckCards.deleteByDeckId(deckId);
            // The bulk delete clears the persistence context, so reload the deck before updating it.
            deck = decks.findById(deckId).orElseThrow();
            deck.setSnapshotId(activeId);
            deck.setOutdated(false);
            deck.setUpdatedAt(Instant.now());
            decks.saveAndFlush(deck);
            deckCards.saveAll(loader.toRows(deck, views));
            return Outcome.MOVED;
        }
        if (!deck.isOutdated()) {
            deck.setOutdated(true);
            decks.saveAndFlush(deck);
            return Outcome.FLAGGED;
        }
        return Outcome.UNCHANGED;
    }
}
