package quorma.backend.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Registration and login. Both return a JWT to send as a Bearer token.")
@SecurityRequirements
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new player and return a JWT")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(responseCode = "400", description = "Invalid username, email or password. Body: `code` = validation.failed, `fieldErrors` = field -> {code, params}.", content = @Content)
    @ApiResponse(responseCode = "409", description = "Username or email already in use. Body: `code` = auth.username_taken, auth.email_taken or auth.credentials_taken")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and return a JWT")
    @ApiResponse(responseCode = "200", description = "Login successful")
    @ApiResponse(responseCode = "400", description = "Missing username or password. Same body as the register 400", content = @Content)
    @ApiResponse(responseCode = "401", description = "Invalid username or password. Body: `code` = auth.invalid_credentials")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request);
    }
}
