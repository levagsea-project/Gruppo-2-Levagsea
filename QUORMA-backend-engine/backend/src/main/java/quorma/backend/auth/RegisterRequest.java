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
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_.-]{3,32}$", message = "3-32 characters: letters, digits, _ . -") String username,
        @NotBlank @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s.]{2,}$", message = "must be a valid email address, e.g. name@example.com") @Size(max = 254, message = "at most 254 characters") String email,
        @NotBlank @Size(min = 8, max = 72) @MaxUtf8Bytes(72) String password) {
}
