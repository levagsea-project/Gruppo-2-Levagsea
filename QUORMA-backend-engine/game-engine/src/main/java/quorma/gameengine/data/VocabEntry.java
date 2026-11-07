package quorma.gameengine.data;

import java.util.List;

/** One entry of a vocab section (effect, trigger, target, condition, duration). {@code raw} keeps every other field. */
public record VocabEntry(String id, String name, String description, List<String> classes, Value.MapV raw) {}
