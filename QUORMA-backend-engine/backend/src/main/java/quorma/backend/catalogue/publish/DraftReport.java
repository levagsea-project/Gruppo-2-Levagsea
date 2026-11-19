package quorma.backend.catalogue.publish;

import java.util.List;
import java.util.UUID;

/**
 * What a draft would do if published (a preview, nothing is written) or what the check found wrong.
 *
 * <p>Codes are stable, language-neutral identifiers (the admin tool translates them); messages are English, for logs.
 * Problems stop the publish, warnings do not.
 *
 * @param draftId  the draft
 * @param ok       true when there is no problem
 * @param problems everything that stops the publish: {@code draft.*} codes from this check, and the engine's own
 *                 {@code card.*}, {@code pool.*} and {@code pack.*} codes (see {@code engine-service/AGENTS.md})
 * @param warnings things worth a look that do not stop it
 * @param factions one entry per faction
 * @param cardsToCreate cards that do not exist yet
 * @param textsToWrite  texts that are missing and will be written (existing texts are never replaced)
 * @param setsToCreate  sets that do not exist yet
 * @param linksToAdd    card-to-set links that will be added
 * @param packTypesToCreate pack types that do not exist yet
 */
public record DraftReport(
        UUID draftId, boolean ok, List<Problem> problems, List<Problem> warnings, List<FactionPreview> factions,
        int cardsToCreate, int textsToWrite, int setsToCreate, int linksToAdd, int packTypesToCreate) {

    public DraftReport {
        problems = List.copyOf(problems);
        warnings = List.copyOf(warnings);
        factions = List.copyOf(factions);
    }

    /**
     * @param code    the kind of problem
     * @param faction the faction it belongs to, or null
     * @param subject the card id, set code or item key it is about, or null
     */
    public record Problem(String code, String faction, String subject, String message) {
    }

    /**
     * What the draft does to one faction's pool.
     *
     * @param touched       true if the draft changes the pool (a new snapshot, or an older one coming back)
     * @param basedOn       the active snapshot the draft was built on (null: none yet)
     * @param added         cards new to the pool
     * @param changed       cards whose definition differs from the active one
     * @param unchanged     cards the draft leaves as they are
     * @param total         cards in the candidate snapshot
     * @param contentHash   hash of the candidate snapshot; null if the faction is not touched
     * @param reactivates   the candidate already exists as an older snapshot, which would become active again
     */
    public record FactionPreview(
            String faction, boolean touched, UUID basedOn, int added, int changed, int unchanged, int total,
            String contentHash, boolean reactivates) {
    }
}
