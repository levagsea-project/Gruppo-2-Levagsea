package quorma.gameengine.data;

import java.util.List;

/** Declaration of a per-card keyword parameter (for example Scudo's {@code x}). */
public record ParamSpec(String type, boolean required, Value defaultValue, List<Value> allowedValues) {}
