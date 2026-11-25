package quorma.backend.catalogue.publish;

import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.user.Role;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;

/**
 * The account that publishes what nobody published: the bootstrap from set files. An admin with no usable password (the stored
 * value is not a BCrypt hash, so no password matches it) and no credits: it cannot log in, it only appears in the audit
 * trail of a snapshot.
 */
@Component
public class SystemUser {

    public static final String USERNAME = "sistema";

    private static final String NO_LOGIN = "!account-without-password";

    private final UserRepository users;

    public SystemUser(UserRepository users) {
        this.users = users;
    }

    /** The id of the system user, created on first use. */
    @Transactional
    public UUID id() {
        return users.findByUsernameIgnoreCase(USERNAME)
                .orElseGet(() -> users.save(new User(USERNAME, "sistema@quorma.local", NO_LOGIN, Role.ADMIN, 0, USERNAME, "avatar-01")))
                .getId();
    }
}
