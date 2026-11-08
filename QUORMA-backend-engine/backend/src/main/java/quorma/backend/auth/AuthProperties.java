package quorma.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code auth.registration-bonus}: credits granted to a new player. */
@ConfigurationProperties(prefix = "auth")
public record AuthProperties(long registrationBonus) {
}
