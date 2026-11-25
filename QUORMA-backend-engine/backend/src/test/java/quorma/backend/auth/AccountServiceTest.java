package quorma.backend.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import quorma.backend.common.ErrorCodes;
import quorma.backend.user.User;
import quorma.backend.user.UserRepository;

/** Changing the password or the email: the current password is checked first, and nothing is written without it. */
class AccountServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final AccountService service = new AccountService(users, encoder);
    private final UUID id = UUID.randomUUID();
    private final User user = mock(User.class);

    AccountServiceTest() {
        when(user.getId()).thenReturn(id);
        when(user.getPasswordHash()).thenReturn(encoder.encode("password-attuale"));
        when(users.findById(id)).thenReturn(Optional.of(user));
    }

    @Test
    void aWrongCurrentPasswordChangesNothing() {
        AuthException e = assertThrows(AuthException.class,
                () -> service.changePassword(id, new ChangePasswordRequest("sbagliata", "nuova-password")));
        assertEquals(HttpStatus.FORBIDDEN, e.status());
        assertEquals(ErrorCodes.CURRENT_PASSWORD_WRONG, e.code());
        verify(users, never()).updatePasswordHash(any(), any());
    }

    @Test
    void theRightCurrentPasswordStoresAHashOfTheNewOne() {
        service.changePassword(id, new ChangePasswordRequest("password-attuale", "nuova-password"));
        verify(users).updatePasswordHash(any(), argThat(h -> encoder.matches("nuova-password", h)));
    }

    @Test
    void anEmailOfAnotherAccountIsRefused() {
        User other = mock(User.class);
        when(other.getId()).thenReturn(UUID.randomUUID());
        when(users.findByEmailIgnoreCase("a@b.it")).thenReturn(Optional.of(other));
        AuthException e = assertThrows(AuthException.class,
                () -> service.changeEmail(id, new ChangeEmailRequest("a@b.it", "password-attuale")));
        assertEquals(HttpStatus.CONFLICT, e.status());
        assertEquals(ErrorCodes.EMAIL_TAKEN, e.code());
        verify(users, never()).updateEmail(any(), any());
    }

    @Test
    void theOwnAddressCanBeWrittenAgain() {
        when(users.findByEmailIgnoreCase("me@b.it")).thenReturn(Optional.of(user));
        assertEquals("me@b.it", service.changeEmail(id, new ChangeEmailRequest("me@b.it", "password-attuale")));
        verify(users).updateEmail(id, "me@b.it");
    }

    @Test
    void aWrongCurrentPasswordDoesNotChangeTheEmail() {
        assertThrows(AuthException.class, () -> service.changeEmail(id, new ChangeEmailRequest("x@y.it", "no")));
        verify(users, never()).updateEmail(any(), any());
    }
}
