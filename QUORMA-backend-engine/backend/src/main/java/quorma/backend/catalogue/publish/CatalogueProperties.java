package quorma.backend.catalogue.publish;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * {@code catalogue.*}: the optional bootstrap from a folder of set files and the size of a batch import.
 *
 * @param bootstrap      the optional bootstrap
 * @param maxImportBytes the most a batch import body may weigh (the base set is about 350 KB)
 */
@ConfigurationProperties(prefix = "catalogue")
public record CatalogueProperties(
        @DefaultValue Bootstrap bootstrap, @DefaultValue("5MB") DataSize maxImportBytes) {

    /**
     * @param path       a folder of set files ({@code data/sets/*.json}) to load at startup; empty means off
     * @param retries    how many times to try if the engine service is not up yet (the compose file only waits for it
     *                   to start)
     * @param retryDelay wait between two tries
     */
    public record Bootstrap(@DefaultValue("") String path, @DefaultValue("10") int retries,
                       @DefaultValue("3s") Duration retryDelay) {
    }

    @Configuration
    @EnableConfigurationProperties(CatalogueProperties.class)
    static class Config {
    }
}
