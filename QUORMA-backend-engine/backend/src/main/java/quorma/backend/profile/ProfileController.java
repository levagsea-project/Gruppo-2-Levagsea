package quorma.backend.profile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Profile", description = "The name and avatar other players see. Players only.")
public class ProfileController {

    private final ProfileService profiles;
    private final ProfileProperties props;

    public ProfileController(ProfileService profiles, ProfileProperties props) {
        this.profiles = profiles;
        this.props = props;
    }

    @GetMapping("/me/profile")
    @Operation(operationId = "getMyProfile", summary = "My profile",
            description = "`username` (the identity, never changes), `displayName` and `avatar` (a code from `GET /avatars`). Every player has both from registration: `displayName` starts as the username and `avatar` as the first code.")
    public ProfileResponse mine(@AuthenticationPrincipal Jwt jwt) {
        return profiles.mine(UUID.fromString(jwt.getSubject()));
    }

    @PutMapping("/me/profile")
    @Operation(operationId = "updateMyProfile", summary = "Change my display name and avatar",
            description = "Replaces both fields and answers with the stored profile. `displayName`: 3 to 32 characters, letters, digits, `_`, `.`, `-` (the username rules); it is not unique. `avatar`: a code from `GET /avatars`.")
    @ApiResponse(responseCode = "400", description = "Body `code` = validation.failed, `fieldErrors` = field -> {code, params}: validation.required, validation.display_name.pattern, validation.avatar.unknown.")
    public ProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return profiles.update(UUID.fromString(jwt.getSubject()), request);
    }

    @GetMapping("/users/{username}/profile")
    @Operation(operationId = "getUserProfile", summary = "Another player's profile",
            description = "`username` (as stored, whatever the case in the URL), `displayName`, `avatar` and `stats`, the same object as `GET /users/{username}/stats`. Any player can read it. 404 `user.not_found` if there is no such player; admins do not play and are not found.")
    @ApiResponse(responseCode = "404", description = "Body `code` = user.not_found.")
    public UserProfileResponse ofUser(@PathVariable String username) {
        return profiles.ofUser(username);
    }

    @GetMapping("/avatars")
    @Operation(operationId = "listAvatars", summary = "The avatars a player can pick",
            description = "The codes of the predefined avatars, in display order. The images are the frontend's; the first code is the one new players get.")
    public List<String> avatars() {
        return props.avatars();
    }
}
