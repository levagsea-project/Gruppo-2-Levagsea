package quorma.backend.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import quorma.backend.common.ErrorCodes;
import quorma.backend.user.CreditTransaction;
import quorma.backend.user.CreditTransactionRepository;
import quorma.backend.user.CreditTransactionType;
import quorma.backend.user.Role;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;

@Service
public class AuthService {

    /** Hash checked when the user is unknown, so login timing does not reveal which usernames exist. */
    private final String dummyHash;

    private final UserRepository users;
    private final CreditTransactionRepository ledger;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuthProperties props;

    public AuthService(UserRepository users, CreditTransactionRepository ledger, PasswordEncoder encoder,
                       JwtService jwt, AuthProperties props) {
        this.users = users;
        this.ledger = ledger;
        this.encoder = encoder;
        this.jwt = jwt;
        this.props = props;
        this.dummyHash = encoder.encode("this-user-does-not-exist");
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (users.existsByUsernameIgnoreCase(req.username())) {
            throw new AuthException(HttpStatus.CONFLICT, ErrorCodes.USERNAME_TAKEN);
        }
        if (users.existsByEmailIgnoreCase(req.email())) {
            throw new AuthException(HttpStatus.CONFLICT, ErrorCodes.EMAIL_TAKEN);
        }
        long bonus = Math.max(0, props.registrationBonus());
        User user = new User(req.username(), req.email(), encoder.encode(req.password()), Role.PLAYER, bonus);
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Lost a race with a concurrent registration (unique indexes on lower(username) and lower(email)).
            throw new AuthException(HttpStatus.CONFLICT, ErrorCodes.CREDENTIALS_TAKEN);
        }
        if (bonus > 0) {
            ledger.save(new CreditTransaction(user.getId(), bonus, bonus, CreditTransactionType.REGISTRATION_BONUS, null, null));
        }
        return respond(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByUsernameIgnoreCase(req.username()).orElse(null);
        boolean ok = encoder.matches(req.password(), user == null ? dummyHash : user.getPasswordHash());
        if (user == null || !ok) {
            throw new AuthException(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_CREDENTIALS);
        }
        return respond(user);
    }

    private AuthResponse respond(User user) {
        return new AuthResponse(jwt.issue(user), "Bearer", jwt.expiresInSeconds(),
                user.getId(), user.getUsername(), user.getRole().name());
    }
}
