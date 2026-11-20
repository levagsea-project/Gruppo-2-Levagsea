package quorma.backend.match;

import java.security.SecureRandom;
import java.util.function.Predicate;

/**
 * Codes shown to players: {@code ch-A5BL-20G8}-style names for challenges and rooms ({@code rnd-...}). The 8
 * characters come from 31 symbols with no 0, 1, I, L or O, so they are easy to read aloud. A code is only a name:
 * it never gives access to anything, the player in the JWT does.
 */
final class CodeGenerator {

    static final String CHALLENGE_PREFIX = "ch";
    static final String ROOM_PREFIX = "rnd";

    private static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";
    private static final SecureRandom RANDOM = new SecureRandom();

    private CodeGenerator() {
    }

    /** A code that {@code taken} says is free; the space is large, so a second try is already rare. */
    static String unique(String prefix, Predicate<String> taken) {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = next(prefix);
            if (!taken.test(code)) {
                return code;
            }
        }
        throw new IllegalStateException("could not find a free " + prefix + " code");
    }

    static String next(String prefix) {
        StringBuilder sb = new StringBuilder(prefix).append('-');
        for (int i = 0; i < 8; i++) {
            if (i == 4) {
                sb.append('-');
            }
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
