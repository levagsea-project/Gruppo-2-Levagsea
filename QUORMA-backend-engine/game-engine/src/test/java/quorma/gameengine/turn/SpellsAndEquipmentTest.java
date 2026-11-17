package quorma.gameengine.turn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.effects.AbilityException;
import quorma.gameengine.log.CreatureRef;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.CardEffectResolved;
import quorma.gameengine.log.StatChange;
import quorma.gameengine.model.AbilityRef;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.Faction;
import quorma.gameengine.model.InlineEffect;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.model.SpellCard;
import quorma.gameengine.model.Subfaction;
import quorma.gameengine.testkit.Boards;
import quorma.gameengine.testkit.TurnResolvers;

/**
 * Spells (10.5) and equipment (10.6) with effects written on the card: expansions of class I with no trigger. A spell
 * aims at a whole side (S9 yours, S10 the opponent's); an equipment at the creature it is applied to (S27). Every
 * expected value is worked out by hand.
 */
class SpellsAndEquipmentTest {

    private final TurnResolver resolver = new TurnResolver(TurnResolvers.RULES);

    private static ExpansionDef immediate(String effect, String target, String duration, int amount) {
        return new ExpansionDef(null, Map.of(), effect,
                Map.of("mode", new Value.StrV("flat"), "amount", new Value.IntV(amount)),
                target, Map.of(), duration, List.of(), ResClass.I);
    }

    private static InlineEffect ability(ExpansionDef... expansions) {
        return new InlineEffect(List.of(expansions));
    }

    private static List<CardEffectResolved> effects(List<MatchEvent> log) {
        return log.stream().filter(e -> e instanceof CardEffectResolved).map(e -> (CardEffectResolved) e).toList();
    }

    @Test
    void aHealingSpellReachesEveryLivingCreatureOfItsOwner() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2V(1000/9000) 2:3F(1000/9000)");
        a.deck().add(new SpellCard("HEAL", Faction.RESISTENZA_UMANA, List.of(ability(immediate("E6", "S9", "D_Istantanea", 1400)))));
        MatchState state = Boards.match(a, Boards.side(Subfaction.SEKTAC, 12000, ""));
        Boards.creature(a, 1).takeDamage(6000); // 3000 left
        Boards.creature(a, 2).takeDamage(500); // 8500 left

        List<MatchEvent> log = new ArrayList<>();
        resolver.refill(state, log);

        assertEquals(4400, Boards.creature(a, 1).currentDef(), "3000 + 1400");
        assertEquals(9000, Boards.creature(a, 2).currentDef(), "8500 + 1400, capped at the base DEF");
        assertEquals(List.of(
                new CardEffectResolved(Side.A, "HEAL", "INLINE_1", "E6", new CreatureRef(Side.A, 1, "2V"),
                        List.of(new StatChange(StatChange.Stat.DEF, 3000, 4400))),
                new CardEffectResolved(Side.A, "HEAL", "INLINE_1", "E6", new CreatureRef(Side.A, 2, "3F"),
                        List.of(new StatChange(StatChange.Stat.DEF, 8500, 9000)))), effects(log));
        int resolved = log.indexOf(new MatchEvent.SpellResolved(Side.A, 3, "HEAL"));
        assertTrue(resolved >= 0 && log.indexOf(effects(log).get(0)) > resolved, "the effects follow SpellResolved");
    }

    @Test
    void aSpellAimedAtTheOpponentReachesTheOpponentsCreatures() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2V(1000/9000)");
        a.deck().add(new SpellCard("MEND", Faction.RESISTENZA_UMANA, List.of(ability(immediate("E6", "S10", "D_Istantanea", 1400)))));
        PlayerState b = Boards.side(Subfaction.SEKTAC, 12000, "1:KS(100/9000)");
        MatchState state = Boards.match(a, b);
        Boards.creature(b, 1).takeDamage(5000); // 4000 left

        resolver.refill(state, new ArrayList<>());

        assertEquals(5400, Boards.creature(b, 1).currentDef());
        assertEquals(9000, Boards.creature(a, 1).currentDef(), "A's own creatures are not reached by S10");
    }

    @Test
    void anEquipmentChangesTheCreatureDrawnAfterIt() {
        // Lane 1 is empty. The equipment is drawn first and waits; the creature drawn next gets it: +500 ATK, +300 DEF.
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "2:3F(1000/9000) 3:4S(1000/9000) 4:5V(1000/9000) 5:6T(1000/9000)");
        a.deck().addAll(0, List.of(
                new EquipmentCard("SWORD", Faction.RESISTENZA_UMANA, List.of(
                        ability(immediate("E1", "S27", "D_Permanente", 500)),
                        ability(immediate("E2", "S27", "D_Permanente", 300)))),
                Boards.deck(Subfaction.ADATTATI, "KV(1000/1000)").get(0)));
        MatchState state = Boards.match(a, Boards.side(Subfaction.SEKTAC, 12000, ""));

        List<MatchEvent> log = new ArrayList<>();
        resolver.refill(state, log);

        assertEquals(1500, Boards.creature(a, 1).currentAtk());
        assertEquals(1300, Boards.creature(a, 1).currentDef());
        assertEquals(List.of(
                new CardEffectResolved(Side.A, "SWORD", "INLINE_1", "E1", new CreatureRef(Side.A, 1, "KV"),
                        List.of(new StatChange(StatChange.Stat.ATK, 1000, 1500))),
                new CardEffectResolved(Side.A, "SWORD", "INLINE_2", "E2", new CreatureRef(Side.A, 1, "KV"),
                        List.of(new StatChange(StatChange.Stat.DEF, 1000, 1300)))), effects(log));
        assertTrue(log.contains(new MatchEvent.EquipmentApplied(Side.A, 1, "SWORD", "KV")));
    }

    @Test
    void whatASpellOrEquipmentMayWriteIsChecked() {
        AbilityBinder binder = new AbilityBinder(TurnResolvers.RULES);
        ExpansionDef triggered = new ExpansionDef("T0", Map.of(), "E6",
                Map.of("mode", new Value.StrV("flat"), "amount", new Value.IntV(1)), "S9", Map.of(),
                "D_Istantanea", List.of(), ResClass.C);

        assertTrue(refused(binder, new SpellCard("X", Faction.RESISTENZA_UMANA, List.of(ability(triggered))))
                .contains("a spell must be class I with no trigger"));
        assertTrue(refused(binder, new SpellCard("X", Faction.RESISTENZA_UMANA, List.of(new AbilityRef("K_SCUDO"))))
                .contains("a spell has its effects written as expansions"));
        assertTrue(refused(binder, new SpellCard("X", Faction.RESISTENZA_UMANA, List.of(ability(immediate("E6", "S27", "D_Istantanea", 1)))))
                .contains("a spell aims at [S10, S9], not S27"));
        assertTrue(refused(binder, new EquipmentCard("X", Faction.RESISTENZA_UMANA, List.of(ability(immediate("E6", "S9", "D_Istantanea", 1)))))
                .contains("an equipment aims at [S27], not S9"));
        assertTrue(refused(binder, new SpellCard("X", Faction.RESISTENZA_UMANA, List.of(ability(immediate("E26", "S9", "D_Istantanea", 1)))))
                .contains("effect E26 is not implemented"));
    }

    @Test
    void aCreatureCannotUseTheImmediateShapes() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 12000, "1:2V(1000/9000)");
        String message = assertThrows(AbilityException.class,
                () -> Boards.give(a, 1, ability(immediate("E6", "S9", "D_Istantanea", 1)))).getMessage();
        assertTrue(message.contains("an expansion with no trigger is for a spell or an equipment"), message);
    }

    private static String refused(AbilityBinder binder, quorma.gameengine.model.Card card) {
        return assertThrows(AbilityException.class, () -> binder.bindImmediate(card)).getMessage();
    }
}
