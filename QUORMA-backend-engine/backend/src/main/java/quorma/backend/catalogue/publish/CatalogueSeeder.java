package quorma.backend.catalogue.publish;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import quorma.backend.catalogue.publish.CatalogueDrift.DriftReport;
import quorma.backend.catalogue.publish.CataloguePublisher.Outcome;
import quorma.backend.catalogue.publish.CataloguePublisher.PublishResult;
import quorma.backend.catalogue.publish.ChangesetImporter.ImportResponse;
import quorma.backend.catalogue.publish.ChangesetImporter.Mode;
import quorma.backend.common.ErrorCodes;
import quorma.backend.engine.EngineUnavailableException;

/**
 * At startup, adds the bundled base set to the catalogue where it is missing: an add-only import by the system user,
 * through the same draft and publish as everything else (the engine checks it like any other batch).
 *
 * <p>It never touches a card that exists: an admin's edit survives a restart with a changed file. Changing existing
 * cards is an explicit upsert import. When the bundled file and the database differ, a warning says so (the same
 * report as {@code GET /admin/catalogue/drift}). The seed never stops the backend from starting: if the engine service
 * is not up it tries again a few times, and any other failure is logged and the seed is simply tried again at the next
 * start.
 */
@Component
public class CatalogueSeeder implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogueSeeder.class);

    public enum SeedOutcome { SEEDED, NOTHING_TO_ADD, SKIPPED_DRAFT_OPEN, FAILED }

    private final CatalogueProperties props;
    private final BundledCatalogue bundled;
    private final ChangesetImporter importer;
    private final CatalogueDrift drift;
    private final SystemUser systemUser;

    public CatalogueSeeder(CatalogueProperties props, BundledCatalogue bundled, ChangesetImporter importer,
                           CatalogueDrift drift, SystemUser systemUser) {
        this.props = props;
        this.bundled = bundled;
        this.importer = importer;
        this.drift = drift;
        this.systemUser = systemUser;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!props.seed().enabled()) {
            LOG.info("catalogue seed is off (catalogue.seed.enabled=false)");
            return;
        }
        seed();
    }

    /** Adds the bundled cards that are missing; never throws. */
    public SeedOutcome seed() {
        Changeset changeset;
        try {
            changeset = bundled.load();
        } catch (RuntimeException e) {
            LOG.error("catalogue seed: the bundled catalogue cannot be read, nothing is seeded", e);
            return SeedOutcome.FAILED;
        }
        SeedOutcome outcome = SeedOutcome.FAILED;
        for (int attempt = 1; attempt <= Math.max(1, props.seed().retries()); attempt++) {
            try {
                outcome = importOnce(changeset);
                break;
            } catch (EngineUnavailableException e) {
                LOG.warn("catalogue seed: the engine service is not available (attempt {} of {}), {}", attempt,
                        props.seed().retries(), attempt < props.seed().retries() ? "trying again" : "giving up until the next start");
                if (attempt < props.seed().retries() && !pause()) {
                    return SeedOutcome.FAILED;
                }
            } catch (CatalogueException e) {
                outcome = handle(e);
                break;
            } catch (RuntimeException e) {
                LOG.error("catalogue seed failed, it will be tried again at the next start", e);
                break;
            }
        }
        warnOnDrift(changeset);
        return outcome;
    }

    private SeedOutcome importOnce(Changeset changeset) {
        ImportResponse response = importer.run(changeset, Mode.ADD_ONLY, false, "seed", systemUser.id(), "startup seed");
        PublishResult r = response.result();
        boolean nothing = r.cardsCreated() == 0 && r.textsWritten() == 0 && r.setsCreated() == 0 && r.linksAdded() == 0
                && r.factions().stream().allMatch(f -> f.outcome() == Outcome.UNCHANGED);
        if (nothing) {
            LOG.info("catalogue seed: the catalogue already has everything the bundled file adds");
            return SeedOutcome.NOTHING_TO_ADD;
        }
        LOG.info("catalogue seed: {} cards, {} texts, {} sets, {} links added; snapshots {}", r.cardsCreated(),
                r.textsWritten(), r.setsCreated(), r.linksAdded(), r.factions());
        return SeedOutcome.SEEDED;
    }

    private SeedOutcome handle(CatalogueException e) {
        if (e.code().equals(ErrorCodes.CATALOGUE_DRAFT_OPEN_EXISTS)) {
            LOG.warn("catalogue seed skipped: a draft is open (an admin is working on one, or another instance is seeding)");
            return SeedOutcome.SKIPPED_DRAFT_OPEN;
        }
        if (e.status() == HttpStatus.UNPROCESSABLE_CONTENT && e.report() != null) {
            LOG.error("catalogue seed: the bundled catalogue does not pass the engine's check, nothing is seeded: {}",
                    e.report().problems());
        } else {
            LOG.error("catalogue seed refused: {} {}", e.code(), e.reason());
        }
        return SeedOutcome.FAILED;
    }

    private boolean pause() {
        try {
            Thread.sleep(props.seed().retryDelay().toMillis());
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void warnOnDrift(Changeset changeset) {
        try {
            DriftReport report = drift.compare(changeset);
            if (!report.changedCards().isEmpty() || !report.missingCards().isEmpty()) {
                LOG.warn("catalogue drift: the bundled file and the database differ ({} cards changed, {} missing); "
                        + "a startup never changes a card that exists, use an upsert import. Changed: {}",
                        report.changedCards().size(), report.missingCards().size(), report.changedCards());
            }
        } catch (RuntimeException e) {
            LOG.warn("catalogue drift check failed", e);
        }
    }
}
