package quorma.backend.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = ErrorCodes.REQUIRED) String username,
                           @NotBlank(message = ErrorCodes.REQUIRED) String password) {
}
