package quorma.backend.catalogue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

/** The {@code Accept-Language} header as a list of languages to try, with Italian always the last fallback. */
class PreferredLocalesTest {

    private static List<String> tags(String header) {
        return PreferredLocales.parse(header).tags();
    }

    private static List<String> candidates(String header) {
        return PreferredLocales.parse(header).candidates();
    }

    @Test
    void noHeaderMeansItalian() {
        assertEquals(List.of(), tags(null));
        assertEquals(List.of(), tags(""));
        assertEquals(List.of(), tags("   "));
        assertEquals(List.of("it"), PreferredLocales.DEFAULT.candidates());
    }

    @Test
    void aRegionalTagIsTriedBeforeItsLanguage() {
        assertEquals(List.of("en-US", "en"), tags("en-US"));
        assertEquals(List.of("pt-BR", "pt"), tags("pt-br"));
        assertEquals(List.of("en"), tags("en"));
        assertEquals(List.of("en-US", "en", "it"), candidates("en-US"));
    }

    @Test
    void theBestWeightComesFirstAndTheOrderOfTiesIsKept() {
        assertEquals(List.of("fr", "en", "it"), tags("it;q=0.3, fr, en;q=0.8"));
        assertEquals(List.of("en-US", "en", "it"), tags("en-US,en;q=0.9,it;q=0.5"));
        assertEquals(List.of("de", "en"), tags("de, en"));
    }

    @Test
    void italianIsTheLastCandidateUnlessAskedForEarlier() {
        assertEquals(List.of("fr", "en", "it"), candidates("fr, en"));
        assertEquals(List.of("it", "en"), candidates("it, en"));
        assertEquals(List.of("en", "it"), candidates("en, it;q=0.1"));
    }

    @Test
    void thingsThatAreNotUsableAreIgnored() {
        assertEquals(List.of(), tags("*"));
        assertEquals(List.of("en"), tags("en, *;q=0.1"));
        assertEquals(List.of("en"), tags("fr;q=0, en;q=0.5"), "q=0 means not acceptable");
        assertEquals(List.of(), tags("zzz"), "a language code is two letters");
        assertEquals(List.of(), tags(";;;"));
        assertEquals(List.of(), tags("en;q=oops"));
        assertEquals(List.of(), tags("x".repeat(600)));
    }

    @Test
    void aLongListIsCutShort() {
        String many = "aa,bb,cc,dd,ee,ff,gg,hh,ii,jj,kk";
        assertEquals(8, tags(many).size());
        assertEquals("aa", tags(many).get(0));
    }
}
