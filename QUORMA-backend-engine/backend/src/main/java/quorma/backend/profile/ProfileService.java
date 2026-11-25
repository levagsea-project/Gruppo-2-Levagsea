package quorma.backend.profile;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.telemetry.StatsService;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;

@Service
public class ProfileService {

    private final UserRepository users;
    private final StatsService stats;

    public ProfileService(UserRepository users, StatsService stats) {
        this.users = users;
        this.stats = stats;
    }

    @Transactional(readOnly = true)
    public ProfileResponse mine(UUID userId) {
        return of(find(userId));
    }

    /** Replaces both fields. Never saves the entity: that would bump the version that guards the credit balance. */
    @Transactional
    public ProfileResponse update(UUID userId, UpdateProfileRequest req) {
        find(userId);
        users.updateProfile(userId, req.displayName(), req.avatar());
        return of(find(userId));
    }

    /**
     * Another player's public profile with their statistics. The player is found by {@link StatsService#findPlayer}
     * (the same rule as the statistics endpoint) and read once; the stored spelling of the username is answered.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse ofUser(String username) {
        User user = stats.findPlayer(username);
        return new UserProfileResponse(user.getUsername(), user.getDisplayName(), user.getAvatar(), stats.forUser(user.getId()));
    }

    static ProfileResponse of(User user) {
        return new ProfileResponse(user.getUsername(), user.getDisplayName(), user.getAvatar(), user.getEmail());
    }

    private User find(UUID userId) {
        return users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("user " + userId + " from a valid JWT not found"));
    }
}
