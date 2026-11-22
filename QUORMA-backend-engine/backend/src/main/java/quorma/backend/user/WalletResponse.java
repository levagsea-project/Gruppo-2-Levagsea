package quorma.backend.user;

import io.swagger.v3.oas.annotations.media.Schema;

/** The player's credit balance. */
public record WalletResponse(
        @Schema(description = "Credits the player holds now; the same number `creditsAfter` reports after a pack purchase") long credits) {
}
