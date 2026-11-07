package quorma.gameengine.data;

import java.util.List;
import java.util.Map;

public record KeywordDef(
        String id, String name, ResClass cls, String balanceRef,
        Map<String, ParamSpec> params, String description,
        List<ExpansionDef> expandsTo) {}
