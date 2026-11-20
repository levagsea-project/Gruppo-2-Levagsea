package quorma.backend.catalogue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The languages a caller prefers for card text, from the {@code Accept-Language} header, best first. The text of a card
 * comes in the first of these that it has; Italian is the fallback and always the last candidate, so a card always has
 * a text.
 *
 * <p>Languages are language tags as the database stores them: {@code it}, {@code en}, {@code pt-BR}. A header tag such as
 * {@code en-US} is tried as {@code en-US}, then {@code en}. {@code *}, a weight of 0 and anything that is not a language
 * tag are ignored, and a header that cannot be read means Italian.
 *
 * @param tags the languages asked for, best first (without the Italian fallback unless it was asked for)
 */
public record PreferredLocales(List<String> tags) {

    /** No preference: Italian. */
    public static final PreferredLocales DEFAULT = new PreferredLocales(List.of());

    /** Most tags kept from a header (the rest are noise). */
    private static final int MAX_TAGS = 8;

    public PreferredLocales {
        tags = List.copyOf(tags);
    }

    /** Reads an {@code Accept-Language} header value; null, blank or unreadable gives {@link #DEFAULT}. */
    public static PreferredLocales parse(String header) {
        if (header == null || header.isBlank() || header.length() > 500) {
            return DEFAULT;
        }
        List<Locale.LanguageRange> ranges;
        try {
            ranges = new ArrayList<>(Locale.LanguageRange.parse(header));
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
        ranges.sort(Comparator.comparingDouble(Locale.LanguageRange::getWeight).reversed());
        Set<String> tags = new LinkedHashSet<>();
        for (Locale.LanguageRange range : ranges) {
            if (range.getWeight() <= 0 || range.getRange().equals("*")) {
                continue;
            }
            String[] parts = range.getRange().split("-");
            if (!parts[0].matches("[a-z]{2}")) {
                continue;
            }
            if (parts.length > 1 && parts[1].matches("[a-z]{2}")) {
                tags.add(parts[0] + "-" + parts[1].toUpperCase(Locale.ROOT));
            }
            tags.add(parts[0]);
            if (tags.size() >= MAX_TAGS) {
                break;
            }
        }
        return new PreferredLocales(new ArrayList<>(tags));
    }

    /** The languages to try in order: the ones asked for, then Italian. */
    public List<String> candidates() {
        List<String> out = new ArrayList<>(tags);
        if (!out.contains(CardTextService.FALLBACK_LOCALE)) {
            out.add(CardTextService.FALLBACK_LOCALE);
        }
        return out;
    }
}
