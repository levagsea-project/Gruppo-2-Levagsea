package quorma.backend.collection;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/collection")
@Tag(name = "Collection", description = "The cards a player owns. Players only: admins do not play.")
public class CollectionController {

    private final CollectionService collection;

    public CollectionController(CollectionService collection) {
        this.collection = collection;
    }

    @GetMapping
    @Operation(summary = "List my cards", description = "One entry per owned card instance, with its card data.")
    public CollectionResponse get(@AuthenticationPrincipal Jwt jwt) {
        return collection.get(UUID.fromString(jwt.getSubject()));
    }
}
