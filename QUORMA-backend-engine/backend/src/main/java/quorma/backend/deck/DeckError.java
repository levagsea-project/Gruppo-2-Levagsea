package quorma.backend.deck;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One broken deck rule: an {@code ErrorCodes} code and the values for the {@code {placeholders}} of its label.
 */
public record DeckError(String code, Map<String, Object> params) {

    public DeckError {
        params = Map.copyOf(params);
    }

    public static DeckError of(String code) {
        return new DeckError(code, Map.of());
    }

    /** Params are given as name, value, name, value... and keep that order. */
    public static DeckError of(String code, Object... nameValuePairs) {
        Map<String, Object> params = new LinkedHashMap<>();
        for (int i = 0; i < nameValuePairs.length; i += 2) {
            params.put((String) nameValuePairs[i], nameValuePairs[i + 1]);
        }
        return new DeckError(code, params);
    }
}
