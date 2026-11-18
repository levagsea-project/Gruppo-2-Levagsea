package quorma.gameengine.effects;

import java.util.ArrayList;
import java.util.List;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.Side;

/**
 * Test double for {@link EffectContext}: answers from fields and records what an effect asked the turn to do, so an
 * {@link Effect} can be tested without a {@code Game}.
 */
class FakeContext implements EffectContext {

    int healPermille = 1000;
    boolean noBonus;
    int adjacentAllies;
    boolean saves;
    boolean hasAbility;
    final List<String> calls = new ArrayList<>();

    @Override
    public AbilityBinder binder() {
        return EffectFixtures.BINDER;
    }

    @Override
    public boolean hasAbility(Creature creature, String abilityId) {
        return hasAbility;
    }

    @Override
    public int healPermille(Creature target) {
        return healPermille;
    }

    @Override
    public boolean noBonus(Creature target) {
        return noBonus;
    }

    @Override
    public int adjacentAllyCount(Side side, Creature creature) {
        return adjacentAllies;
    }

    @Override
    public List<StatChange> damage(Side sourceSide, Creature source, Side side, Creature target, int amount,
            Followups after) {
        calls.add("damage " + amount + " from " + sourceSide + " to " + side);
        return List.of(new StatChange(StatChange.Stat.DEF, target.currentDef(), target.currentDef()));
    }

    @Override
    public void destroy(Side sourceSide, Creature source, Side side, Creature target, Followups after) {
        calls.add("destroy " + side);
    }

    @Override
    public void destroyed(Side side, Creature target, Side sourceSide, Creature source, Followups after) {
        calls.add("destroyed " + side + " by " + sourceSide);
    }

    @Override
    public void discard(Side side, Creature target, Followups after) {
        calls.add("discard " + side);
    }

    @Override
    public boolean saveFromDestruction(Side side, Creature target, Followups after) {
        calls.add("save? " + side);
        return saves;
    }
}
