package quorma.backend.auth;

import jakarta.validation.constraints.NotBlank;
import quorma.backend.common.ErrorCodes;

public record LoginRequest(@NotBlank(message = ErrorCodes.REQUIRED) String username,
                           @NotBlank(message = ErrorCodes.REQUIRED) String password) {
}
