package quorma.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Registration payload. Username and email limits mirror the CHECK constraints of the users table. The password
 * is limited in bytes too: BCrypt ignores everything after byte 72, so a longer multi-byte password is rejected.
 */
public record RegisterRequest(
        @NotBlank(message = ErrorCodes.REQUIRED) @Pattern(regexp = "^[A-Za-z0-9_.-]{3,32}$", message = ErrorCodes.USERNAME_PATTERN) String username,
        @NotBlank(message = ErrorCodes.REQUIRED) @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s.]{2,}$", message = ErrorCodes.EMAIL_INVALID) @Size(max = 254, message = ErrorCodes.EMAIL_MAX_LENGTH) String email,
        @NotBlank(message = ErrorCodes.REQUIRED) @Size(min = 8, max = 72, message = ErrorCodes.PASSWORD_SIZE) @MaxUtf8Bytes(72) String password) {
}
