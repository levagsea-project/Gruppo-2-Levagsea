package quorma.backend.catalogue.publish;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
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
 * Loads set files into a database that lacks them: for an initial deployment, or to rebuild the database from zero.
 * It runs at startup only when {@code catalogue.bootstrap.path} names a folder (off otherwise), and nothing is bundled
 * into the jar: the set files ({@code data/sets/*.json}) are mounted next to the backend, or not given at all. Once a
 * set is loaded the database is the only truth, and the sets that come later are added by an admin through the API or
 * by importing a file.
 *
 * <p>All the files of the folder are merged into one batch ({@link Changeset#merge}) and imported add-only by the system
 * user, through the same draft and publish as everything else, so the engine checks it like any other batch and the load
 * is one transaction that does not depend on the order of the files. It never touches a card that exists. When the files
 * and the database differ, a warning says so (the same report as {@code POST /admin/catalogue/drift}). It never stops
 * the backend from starting: if the engine service is not up it tries again a few times, and any other failure is
 * logged and tried again at the next start.
 */
@Component
public class CatalogueBootstrap implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(CatalogueBootstrap.class);

    public enum BootstrapOutcome { LOADED, NOTHING_TO_ADD, SKIPPED_DRAFT_OPEN, FAILED }

    private final CatalogueProperties props;
    private final ChangesetImporter importer;
    private final CatalogueDrift drift;
    private final SystemUser systemUser;

    public CatalogueBootstrap(CatalogueProperties props, ChangesetImporter importer, CatalogueDrift drift,
                              SystemUser systemUser) {
        this.props = props;
        this.importer = importer;
        this.drift = drift;
        this.systemUser = systemUser;
    }

    @Override
    public void run(ApplicationArguments args) {
        String path = props.bootstrap().path();
        if (path == null || path.isBlank()) {
            LOG.info("catalogue bootstrap is off (catalogue.bootstrap.path is not set): sets are added through the admin API");
            return;
        }
        bootstrap(Path.of(path));
    }

    /** Loads the set files of {@code dir} where the database lacks them; never throws. */
    public BootstrapOutcome bootstrap(Path dir) {
        Changeset merged;
        try {
            merged = read(dir);
        } catch (RuntimeException | IOException e) {
            LOG.error("catalogue bootstrap: the set files of {} cannot be read, nothing is loaded", dir, e);
            return BootstrapOutcome.FAILED;
        }
        BootstrapOutcome outcome = BootstrapOutcome.FAILED;
        for (int attempt = 1; attempt <= Math.max(1, props.bootstrap().retries()); attempt++) {
            try {
                outcome = importOnce(merged);
                break;
            } catch (EngineUnavailableException e) {
                LOG.warn("catalogue bootstrap: the engine service is not available (attempt {} of {}), {}", attempt,
                        props.bootstrap().retries(),
                        attempt < props.bootstrap().retries() ? "trying again" : "giving up until the next start");
                if (attempt < props.bootstrap().retries() && !pause()) {
                    return BootstrapOutcome.FAILED;
                }
            } catch (CatalogueException e) {
                outcome = handle(e);
                break;
            } catch (RuntimeException e) {
                LOG.error("catalogue bootstrap failed, it will be tried again at the next start", e);
                break;
            }
        }
        warnOnDrift(merged);
        return outcome;
    }

    private Changeset read(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException(dir + " is not a folder");
        }
        List<Path> files;
        try (Stream<Path> list = Files.list(dir)) {
            files = list.filter(f -> f.getFileName().toString().endsWith(".json")).sorted().toList();
        }
        if (files.isEmpty()) {
            throw new IllegalStateException("no *.json set file in " + dir);
        }
        List<Changeset> parts = new ArrayList<>();
        for (Path file : files) {
            try {
                parts.add(Changeset.parse(DraftPlanner.MAPPER.readTree(Files.readString(file))));
            } catch (CatalogueException e) {
                throw new IllegalStateException(file.getFileName() + ": " + e.reason(), e);
            }
        }
        LOG.info("catalogue bootstrap: {} set file(s) in {}", files.size(), dir);
        return Changeset.merge(parts);
    }

    private BootstrapOutcome importOnce(Changeset changeset) {
        ImportResponse response = importer.run(changeset, Mode.ADD_ONLY, false, "seed", systemUser.id(), "bootstrap");
        PublishResult r = response.result();
        boolean nothing = r.cardsCreated() == 0 && r.textsWritten() == 0 && r.setsCreated() == 0 && r.linksAdded() == 0
                && r.packTypesCreated() == 0
                && r.factions().stream().allMatch(f -> f.outcome() == Outcome.UNCHANGED);
        if (nothing) {
            LOG.info("catalogue bootstrap: the database already has everything the set files add");
            return BootstrapOutcome.NOTHING_TO_ADD;
        }
        LOG.info("catalogue bootstrap: {} cards, {} texts, {} sets, {} pack types, {} links added; snapshots {}", r.cardsCreated(),
                r.textsWritten(), r.setsCreated(), r.packTypesCreated(), r.linksAdded(), r.factions());
        return BootstrapOutcome.LOADED;
    }

    private BootstrapOutcome handle(CatalogueException e) {
        if (e.code().equals(ErrorCodes.CATALOGUE_DRAFT_OPEN_EXISTS)) {
            LOG.warn("catalogue bootstrap skipped: a draft is open (an admin is working on one, or another instance is loading)");
            return BootstrapOutcome.SKIPPED_DRAFT_OPEN;
        }
        if (e.status() == HttpStatus.UNPROCESSABLE_CONTENT && e.report() != null) {
            LOG.error("catalogue bootstrap: the set files do not pass the engine's check, nothing is loaded: {}",
                    e.report().problems());
        } else {
            LOG.error("catalogue bootstrap refused: {} {}", e.code(), e.reason());
        }
        return BootstrapOutcome.FAILED;
    }

    private boolean pause() {
        try {
            Thread.sleep(props.bootstrap().retryDelay().toMillis());
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
                LOG.warn("catalogue drift: the set files and the database differ ({} cards changed, {} missing); "
                        + "a bootstrap never changes a card that exists, use an upsert import. Changed: {}",
                        report.changedCards().size(), report.missingCards().size(), report.changedCards());
            }
        } catch (RuntimeException e) {
            LOG.warn("catalogue drift check failed", e);
        }
    }
}
