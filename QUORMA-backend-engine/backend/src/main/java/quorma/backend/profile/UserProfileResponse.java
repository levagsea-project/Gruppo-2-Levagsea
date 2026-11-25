package quorma.backend.profile;

import quorma.backend.telemetry.StatsResponse;

/** Another player's profile: the public identity plus the statistics of {@code GET /users/{username}/stats}. */
public record UserProfileResponse(String username, String displayName, String avatar, StatsResponse stats) {
}
