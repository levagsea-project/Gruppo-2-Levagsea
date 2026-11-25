package quorma.backend.profile;

/** A player's public identity: the username never changes, the display name and the avatar do. */
public record ProfileResponse(String username, String displayName, String avatar) {
}
