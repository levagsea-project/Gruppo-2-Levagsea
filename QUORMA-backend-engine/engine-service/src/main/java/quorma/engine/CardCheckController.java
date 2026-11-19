package quorma.engine;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /v1/cards/check}: checks a candidate card pool of one faction against the running engine's rules.
 * Always 200 with a report, even when cards are wrong (a wrong card is the answer, not a failed request); a broken
 * request is a 400, as for {@code /v1/simulate}. Not exposed to the outside: only the backend calls it.
 */
@RestController
@RequestMapping("/v1")
public class CardCheckController {

    private final CardCheckService cardCheck;

    public CardCheckController(CardCheckService cardCheck) {
        this.cardCheck = cardCheck;
    }

    @PostMapping(value = "/cards/check", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> check(@RequestBody String requestJson) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(cardCheck.check(requestJson));
    }
}
