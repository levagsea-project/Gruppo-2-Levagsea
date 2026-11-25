package quorma.backend.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import quorma.backend.common.ErrorCodes;

/** Both fields are replaced. The display name follows the username rules (so it never has spaces to trim). */
public record UpdateProfileRequest(
        @NotBlank(message = ErrorCodes.REQUIRED) @Pattern(regexp = "^[A-Za-z0-9_.-]{3,32}$", message = ErrorCodes.DISPLAY_NAME_PATTERN) String displayName,
        @NotBlank(message = ErrorCodes.REQUIRED) @KnownAvatar(message = ErrorCodes.AVATAR_UNKNOWN) String avatar) {
}
