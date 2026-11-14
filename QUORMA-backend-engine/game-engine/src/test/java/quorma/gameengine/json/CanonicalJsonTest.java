package quorma.gameengine.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * The canonical form is Python's {@code json.dumps(value, ensure_ascii=False, indent=2) + "\n"}. Expected texts are
 * written by hand from that rule (and match what Python prints for the same values).
 */
class CanonicalJsonTest {

    private static Map<String, Object> map(Object... keyValue) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < keyValue.length; i += 2) {
            m.put((String) keyValue[i], keyValue[i + 1]);
        }
        return m;
    }

    @Test
    void nestedValuesAreIndentedByTwoSpacesInInsertionOrder() {
        Object value = map("b", 1, "a", List.of(true, false), "c", map("x", null, "y", -7L));
        assertEquals("""
                {
                  "b": 1,
                  "a": [
                    true,
                    false
                  ],
                  "c": {
                    "x": null,
                    "y": -7
                  }
                }
                """, CanonicalJson.write(value));
    }

    @Test
    void emptyContainersStayOnOneLine() {
        assertEquals("{\n  \"m\": {},\n  \"l\": []\n}\n", CanonicalJson.write(map("m", map(), "l", List.of())));
        assertEquals("[]\n", CanonicalJson.write(List.of()));
    }

    @Test
    void onlyQuotesBackslashesAndControlCharactersAreEscaped() {
        assertEquals("\"a\\\"b\\\\c\\nd\\te\\u0001f\"\n", CanonicalJson.write("a\"b\\c\nd\te\u0001f"));
        assertEquals("\"Né × già /  \"\n", CanonicalJson.write("Né × già /  "), "non-ASCII as is, slash not escaped");
    }

    @Test
    void decimalsKeepTheirExactText() {
        assertEquals("[\n  0.12,\n  1.2,\n  0.0012\n]\n",
                CanonicalJson.write(List.of(new BigDecimal("0.12"), new BigDecimal("1.2"), new BigDecimal("0.0012"))));
    }

    @Test
    void nullsInsideListsAreWritten() {
        assertEquals("[\n  null,\n  1\n]\n", CanonicalJson.write(Arrays.asList(null, 1)));
    }

    @Test
    void sha256IsOfTheCanonicalBytes() {
        // printf '{}\n' | sha256sum
        assertEquals("ca3d163bab055381827226140568f3bef7eaac187cebd76878e0b63e9e442356", CanonicalJson.sha256(map()));
    }

    @Test
    void anythingElseIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> CanonicalJson.write(1.5));
        assertThrows(IllegalArgumentException.class, () -> CanonicalJson.write(Map.of(1, 2)));
    }
}
