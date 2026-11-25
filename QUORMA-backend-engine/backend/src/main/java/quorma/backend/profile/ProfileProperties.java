package quorma.backend.profile;

import java.util.HashSet;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code profile.avatars}: the codes of the predefined avatars (the images live in the frontend). The first one is
 * given to every new player. The list must not be empty and must have no duplicates.
 */
@ConfigurationProperties(prefix = "profile")
public record ProfileProperties(List<String> avatars) {

    public ProfileProperties {
        avatars = avatars == null ? List.of() : List.copyOf(avatars);
        if (avatars.isEmpty()) {
            throw new IllegalArgumentException("profile.avatars must list at least one avatar code");
        }
        if (new HashSet<>(avatars).size() != avatars.size()) {
            throw new IllegalArgumentException("profile.avatars has a duplicate code");
        }
        for (String code : avatars) {
            if (code == null || !code.matches("^[a-z0-9_-]{1,32}$")) {
                throw new IllegalArgumentException("profile.avatars: \"" + code + "\" is not a valid code (a-z, 0-9, _ and -, up to 32)");
            }
        }
    }

    public String defaultAvatar() {
        return avatars.get(0);
    }

    public boolean isKnown(String code) {
        return avatars.contains(code);
    }
}
