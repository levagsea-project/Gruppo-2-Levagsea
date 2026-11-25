package quorma.backend.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import quorma.backend.common.ErrorCodes;

/** The new password follows the registration rules (8 to 72 characters, at most 72 bytes: BCrypt reads no more). */
public record ChangePasswordRequest(
        @NotBlank(message = ErrorCodes.REQUIRED) String currentPassword,
        @NotBlank(message = ErrorCodes.REQUIRED) @Size(min = 8, max = 72, message = ErrorCodes.PASSWORD_SIZE) @MaxUtf8Bytes(72) String newPassword) {
}
