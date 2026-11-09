package quorma.gameengine.data;

import java.util.Map;

/** The vocabulary of {@code vocab.json}: what can be expressed. */
public record Vocab(
        Map<String, VocabEntry> effects,
        Map<String, VocabEntry> triggers,
        Map<String, VocabEntry> targets,
        Map<String, VocabEntry> conditions,
        Map<String, VocabEntry> durations) {}
