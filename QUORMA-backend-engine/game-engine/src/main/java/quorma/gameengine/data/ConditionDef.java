package quorma.gameengine.data;

import java.util.Map;

public record ConditionDef(String id, Map<String, Value> params) {}
