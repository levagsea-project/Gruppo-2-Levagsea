package quorma.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code jwt.secret} (HS256, at least 32 bytes) and {@code jwt.expiration} in milliseconds. */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, long expiration) {
}
