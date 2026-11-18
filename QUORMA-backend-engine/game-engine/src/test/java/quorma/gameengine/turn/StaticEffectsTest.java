package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ConditionDef;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;
import quorma.gameengine.effects.AbilityException;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.AttackResolved;
import quorma.gameengine.log.Modifier;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * The static effects of the suit synergies are ordinary abilities, so a card can have them too: the same shapes
 * written on a creature, with no synergy involved. A is Adattati, B is SEKTAC (no advantage); A has a high card with
 * the King in lane 2 as its only attacker. Every expected value is worked out by hand.
 */
class StaticEffectsTest {

    private final TurnResolver resolver = new TurnResolver(TurnResolvers.RULES);

    private static ExpansionDef staticAtk(String target, Map<String, Value> targetParams, int amount, ConditionDef... conditions) {
        return new ExpansionDef("T0", Map.of(), "E1",
                Map.of("mode", new Value.StrV("flat"), "amount", new Value.IntV(amount)),
                target, targetParams, "D_FincheSorgenteInGioco", List.of(conditions), ResClass.C);
    }

    private AttackResolved blowOfA(ExpansionDef expansion) {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2F(1000/9000) 2:KS(1000/9000)");
        Boards.give(a, 1, new InlineEffect(List.of(expansion)));
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, "1:9F(100/9000) 2:5T(100/9000)");
        MatchState state = Boards.match(a, b);
        Evaluation evaluation = resolver.evaluate(state);
        List<MatchEvent> log = new ArrayList<>();
        resolver.cycle1(state, evaluation, resolver.declare(state, evaluation), log);
        return log.stream().filter(e -> e instanceof AttackResolved).map(e -> (AttackResolved) e)
                .filter(e -> e.attack().attacker() == Side.A).findFirst().orElseThrow();
    }

    private static Modifier bonus(int lane, String card, int value) {
        return new Modifier(new CreatureRef(Side.A, lane, card), "INLINE_1", "E1", Modifier.Kind.ATK_BONUS, value);
    }

    @Test
    void aCardGivesItsNeighbourAttackerATeamBonus() {
        // The 2 in lane 1 gives +300 to every adjacent attacker: the King in lane 2 attacks with 1000 + 300.
        AttackResolved blow = blowOfA(staticAtk("S41", Map.of("filters", new Value.ListV(List.of(new Value.StrV("attacker")))), 300));

        assertEquals(1300, blow.atk());
        assertEquals(List.of(bonus(1, "2F", 300)), blow.modifiers());
    }

    @Test
    void theAttackerFilterLeavesOutACreatureThatDoesNotAttack() {
        // Lane 2 is the attacker; the bonus given by lane 2 to its neighbour (lane 1, not an attacker) is not used,
        // and a source with no matching receiver adds nothing.
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2F(1000/9000) 2:KS(1000/9000)");
        Boards.give(a, 2, new InlineEffect(List.of(
                staticAtk("S41", Map.of("filters", new Value.ListV(List.of(new Value.StrV("attacker")))), 300))));
        MatchState state = Boards.match(a, Boards.side(Subfaction.SEKTAC, 12000, "1:9F(100/9000) 2:5T(100/9000)"));
        Evaluation evaluation = resolver.evaluate(state);
        List<MatchEvent> log = new ArrayList<>();
        resolver.cycle1(state, evaluation, resolver.declare(state, evaluation), log);
        AttackResolved blow = log.stream().filter(e -> e instanceof AttackResolved).map(e -> (AttackResolved) e)
                .filter(e -> e.attack().attacker() == Side.A).findFirst().orElseThrow();

        assertEquals(1000, blow.atk(), "S41 never targets the source itself");
    }

    @Test
    void aCardCanBoostItselfOnlyWhileItAttacks() {
        // C1: the source is an attacker. The King attacks, so it gets the bonus; the 2 in lane 1 does not attack.
        ConditionDef isAttacker = new ConditionDef("C1", Map.of());
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2F(1000/9000) 2:KS(1000/9000)");
        Boards.give(a, 1, new InlineEffect(List.of(staticAtk("S1", Map.of(), 500, isAttacker))));
        Boards.give(a, 2, new InlineEffect(List.of(staticAtk("S1", Map.of(), 700, isAttacker))));
        MatchState state = Boards.match(a, Boards.side(Subfaction.SEKTAC, 12000, "1:9F(100/9000) 2:5T(100/9000)"));
        Evaluation evaluation = resolver.evaluate(state);
        List<MatchEvent> log = new ArrayList<>();
        resolver.cycle1(state, evaluation, resolver.declare(state, evaluation), log);
        AttackResolved blow = log.stream().filter(e -> e instanceof AttackResolved).map(e -> (AttackResolved) e)
                .filter(e -> e.attack().attacker() == Side.A).findFirst().orElseThrow();

        assertEquals(1700, blow.atk());
        assertEquals(List.of(new Modifier(new CreatureRef(Side.A, 2, "KS"), "INLINE_1", "E1", Modifier.Kind.ATK_BONUS, 700)),
                blow.modifiers());
    }

    @Test
    void aConditionTheEngineDoesNotKnowIsRefusedWhenTheCardIsBound() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2F(1000/9000)");
        ExpansionDef unknown = staticAtk("S1", Map.of(), 500, new ConditionDef("C99", Map.of()));
        AbilityException e = org.junit.jupiter.api.Assertions.assertThrows(AbilityException.class,
                () -> Boards.give(a, 1, new InlineEffect(List.of(unknown))));
        assertTrue(e.getMessage().contains("condition C99 is not implemented"), e.getMessage());
    }
}
