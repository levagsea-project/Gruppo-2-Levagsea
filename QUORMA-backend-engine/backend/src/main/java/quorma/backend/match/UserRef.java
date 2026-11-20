package quorma.backend.match;

import java.util.UUID;

/** A player as the other player sees them: id and nickname. */
public record UserRef(UUID id, String username) {
}
