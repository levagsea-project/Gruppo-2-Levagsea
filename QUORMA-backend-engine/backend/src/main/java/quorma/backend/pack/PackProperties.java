package quorma.backend.pack;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code packs.types}: the pack types on sale, by code (see application.yml). All numbers are placeholders. */
@ConfigurationProperties(prefix = "packs")
public record PackProperties(Map<String, PackType> types) {
}
