package quorma.gameengine.turn;

import java.util.IdentityHashMap;
import java.util.Map;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.Side;

/**
 * The values some conditions read "at the start of the cycle" (C22, ruleset 7.10.6): each creature's DEF, and its
 * ATK without the bonuses that depend on them. Taken once when a cycle starts, so a blow of the same cycle does not
 * change what a later blow of that cycle compares.
 */
final class CycleStart {

    private final Map<Creature, Integer> defs = new IdentityHashMap<>();
    private final Map<Creature, Integer> atks = new IdentityHashMap<>();

    private CycleStart() {}

    static CycleStart of(MatchState state, Evaluation evaluation) {
        CycleStart start = new CycleStart();
        for (Side side : Side.values()) {
            for (Creature c : state.player(side).creatures()) {
                start.defs.put(c, c.currentDef());
                int atk = c.currentAtk();
                // static bonuses that do not need a target; the ones that do (C22) are what this snapshot is for
                for (var bonus : StaticEffects.atkBonuses(state, evaluation, side, c, null, null)) {
                    atk = Math.addExact(atk, bonus.value());
                }
                start.atks.put(c, atk);
            }
        }
        return start;
    }

    int def(Creature c) {
        return defs.getOrDefault(c, c.currentDef());
    }

    int atk(Creature c) {
        return atks.getOrDefault(c, c.currentAtk());
    }
}
