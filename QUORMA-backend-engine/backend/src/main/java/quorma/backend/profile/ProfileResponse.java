package quorma.backend.profile;

/**
 * A player's own profile: the username never changes, the display name, the avatar and the email do. The email is
 * private: it is only in the player's own profile, never in {@link UserProfileResponse}.
 */
public record ProfileResponse(String username, String displayName, String avatar, String email) {
}
