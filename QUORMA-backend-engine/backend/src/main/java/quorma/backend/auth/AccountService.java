package quorma.backend.auth;

import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.common.ErrorCodes;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;

/**
 * Changing the email or the password of one's own account. Both ask for the current password. Both write with a plain
 * UPDATE: saving the entity would bump {@code users.version}, which guards the credit balance.
 */
@Service
public class AccountService {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AccountService(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        User user = verified(userId, req.currentPassword());
        users.updatePasswordHash(user.getId(), encoder.encode(req.newPassword()));
    }

    /** Returns the stored email. Writing the player's own address again is allowed (so is changing its case). */
    @Transactional
    public String changeEmail(UUID userId, ChangeEmailRequest req) {
        User user = verified(userId, req.currentPassword());
        users.findByEmailIgnoreCase(req.email()).filter(other -> !other.getId().equals(user.getId())).ifPresent(other -> {
            throw new AuthException(HttpStatus.CONFLICT, ErrorCodes.EMAIL_TAKEN);
        });
        try {
            users.updateEmail(user.getId(), req.email());
            users.flush();
        } catch (DataIntegrityViolationException e) {
            // Lost a race with another change or registration (unique index on lower(email)).
            throw new AuthException(HttpStatus.CONFLICT, ErrorCodes.EMAIL_TAKEN);
        }
        return req.email();
    }

    private User verified(UUID userId, String currentPassword) {
        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("user " + userId + " from a valid JWT not found"));
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            // 403, not 401: the token is fine, and a 401 would make the frontend log the player out.
            throw new AuthException(HttpStatus.FORBIDDEN, ErrorCodes.CURRENT_PASSWORD_WRONG);
        }
        return user;
    }
}
