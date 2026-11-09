package quorma.backend.auth;

import java.util.UUID;

/** Returned by register and login: the bearer token plus who it belongs to. */
public record AuthResponse(String token, String tokenType, long expiresInSeconds, UUID userId, String username, String role) {
}
