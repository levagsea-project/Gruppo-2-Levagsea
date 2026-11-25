package quorma.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import quorma.backend.common.ErrorCodes;

/** The email follows the registration rules. The current password proves it is the owner at the keyboard. */
public record ChangeEmailRequest(
        @NotBlank(message = ErrorCodes.REQUIRED) @Email(regexp = "^[^@\\s]+@[^@\\s]+\\.[^@\\s.]{2,}$", message = ErrorCodes.EMAIL_INVALID) @Size(max = 254, message = ErrorCodes.EMAIL_MAX_LENGTH) String email,
        @NotBlank(message = ErrorCodes.REQUIRED) String currentPassword) {
}
