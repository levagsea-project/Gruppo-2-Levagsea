package quorma.backend.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Account", description = "A player changes their own email or password. Both ask for the current password. Players only.")
public class AccountController {

    private final AccountService account;

    public AccountController(AccountService account) {
        this.account = account;
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "changeMyPassword", summary = "Change my password",
            description = "`currentPassword` and `newPassword` (8 to 72 characters, at most 72 bytes in UTF-8, as at registration). Answers 204 with no body. Tokens already issued stay valid until they expire: the API has no token revocation.")
    @ApiResponse(responseCode = "400", description = "Body `code` = validation.failed, `fieldErrors` = field -> {code, params}: validation.required, validation.password.size, validation.max_utf8_bytes.")
    @ApiResponse(responseCode = "403", description = "Body `code` = auth.current_password_wrong. The token is fine: do not log the player out.")
    public void changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        account.changePassword(UUID.fromString(jwt.getSubject()), request);
    }

    @PutMapping("/me/email")
    @Operation(operationId = "changeMyEmail", summary = "Change my email",
            description = "`email` (the registration rules) and `currentPassword`. Answers `{email}` as stored. The new address is not verified: the backend sends no email. Writing the current address again is accepted.")
    @ApiResponse(responseCode = "400", description = "Body `code` = validation.failed, `fieldErrors` = field -> {code, params}: validation.required, validation.email.invalid, validation.email.max_length.")
    @ApiResponse(responseCode = "403", description = "Body `code` = auth.current_password_wrong. The token is fine: do not log the player out.")
    @ApiResponse(responseCode = "409", description = "Body `code` = auth.email_taken: another account has that address (case-insensitive).")
    public EmailResponse changeEmail(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangeEmailRequest request) {
        return new EmailResponse(account.changeEmail(UUID.fromString(jwt.getSubject()), request));
    }
}
