package quorma.backend.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet")
@Tag(name = "Wallet", description = "The credits a player holds. Players only: admins hold no credits and do not play.")
public class WalletController {

    private final UserRepository users;

    public WalletController(UserRepository users) {
        this.users = users;
    }

    @GetMapping
    @Operation(operationId = "getWallet", summary = "My credit balance",
            description = "The current balance. Credits move only by a registration bonus, a pack purchase, a trade, a match reward or an adjustment; a pack purchase also returns the balance it leaves (`creditsAfter`).")
    public WalletResponse get(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalStateException("user " + userId + " from a valid JWT not found"));
        return new WalletResponse(user.getCredits());
    }
}
