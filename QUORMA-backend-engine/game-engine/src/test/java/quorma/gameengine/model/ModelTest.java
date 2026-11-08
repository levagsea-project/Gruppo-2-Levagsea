package quorma.gameengine.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ExpansionDef;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.testkit.Boards;

/** The rules of the state classes themselves: destroyed flag, targetable lanes, cycles, turn counter. */
class ModelTest {

    private static CreatureCard card(int atk, int def) {
        return new CreatureCard("9F", 9, Suit.FRATTURA, Subfaction.ADATTATI, atk, def);
    }

    // ---- Creature: damage and the destroyed flag (ruleset 3.2.5, 3.2.6) ----

    @Test
    void damageLowersDefAndStaysBelowTheKillLine() {
        Creature c = new Creature(card(1000, 3000), 2);
        c.takeDamage(2999);
        assertEquals(1, c.currentDef());
        assertFalse(c.destroyed());
    }

    @Test
    void defOfZeroDestroysAndExtraDamageKeepsItDestroyed() {
        Creature c = new Creature(card(1000, 3000), 2);
        c.takeDamage(3000);
        assertEquals(0, c.currentDef());
        assertTrue(c.destroyed());
        c.takeDamage(500);
        assertEquals(-500, c.currentDef(), "DEF may go below zero");
        assertTrue(c.destroyed());
    }

    @Test
    void damageAddsUpAcrossHits() {
        Creature c = new Creature(card(1000, 3000), 2);
        c.takeDamage(1000);
        c.takeDamage(1000);
        assertEquals(1000, c.currentDef());
        assertFalse(c.destroyed());
        c.takeDamage(1000);
        assertTrue(c.destroyed());
    }

    @Test
    void markDestroyedKeepsDef() {
        Creature c = new Creature(card(1000, 3000), 2);
        c.markDestroyed();
        assertTrue(c.destroyed());
        assertEquals(3000, c.currentDef());
    }

    @Test
    void negativeDamageIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Creature(card(1, 1), 1).takeDamage(-1));
    }

    @Test
    void laneMustBeOneToFive() {
        assertThrows(IllegalArgumentException.class, () -> new Creature(card(1, 1), 0));
        assertThrows(IllegalArgumentException.class, () -> new Creature(card(1, 1), 6));
    }

    // ---- cards ----

    @Test
    void creatureCardRejectsImpossibleValues() {
        assertThrows(IllegalArgumentException.class, () -> new CreatureCard("x", 0, Suit.SPIRA, Subfaction.YLMOR, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new CreatureCard("x", 14, Suit.SPIRA, Subfaction.YLMOR, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new CreatureCard("x", 5, Suit.SPIRA, Subfaction.YLMOR, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new CreatureCard("x", 5, Suit.SPIRA, Subfaction.YLMOR, 1, 0));
    }

    @Test
    void anAtkOfZeroIsAllowed() {
        assertEquals(0, new CreatureCard("x", 5, Suit.SPIRA, Subfaction.YLMOR, 0, 1).atk());
    }

    @Test
    void theFactionOfACreatureComesFromItsSubfaction() {
        assertEquals(Faction.RESISTENZA_UMANA, card(1, 1).faction());
        assertEquals(Faction.POTENZE_ARTIFICIALI, new CreatureCard("x", 5, Suit.SPIRA, Subfaction.YLMOR, 1, 1).faction());
    }

    @Test
    void everySubfactionHasTheFactionOfTheRuleset() {
        for (Subfaction s : List.of(Subfaction.ADATTATI, Subfaction.SPEZZASIGILLI, Subfaction.PORTANOME, Subfaction.SEGNATERRA)) {
            assertEquals(Faction.RESISTENZA_UMANA, s.faction(), s.name());
        }
        for (Subfaction s : List.of(Subfaction.SEKTAC, Subfaction.MEROZYN, Subfaction.EFFRIX, Subfaction.YLMOR)) {
            assertEquals(Faction.POTENZE_ARTIFICIALI, s.faction(), s.name());
        }
    }

    @Test
    void subfactionDataNamesRoundTrip() {
        for (Subfaction s : Subfaction.values()) {
            assertEquals(s, Subfaction.fromDataName(s.dataName()));
        }
        assertEquals("Adattati", Subfaction.ADATTATI.dataName());
        assertEquals("SEKTAC", Subfaction.SEKTAC.dataName());
        assertThrows(IllegalArgumentException.class, () -> Subfaction.fromDataName("Nope"));
    }

    @Test
    void potenzeArtificialiComeBeforeResistenzaUmanaInThePriorityOrder() {
        assertTrue(Faction.POTENZE_ARTIFICIALI.ordinal() < Faction.RESISTENZA_UMANA.ordinal());
    }

    // ---- abilities ----

    @Test
    void everyKindOfCardHasAbilities() {
        AbilityRef scudo = new AbilityRef("K_SCUDO", Map.of("x", new Value.IntV(15)));
        CreatureCard creature = new CreatureCard("x", 5, Suit.SPIRA, Subfaction.YLMOR, 1, 1, List.of(scudo));
        SpellCard spell = new SpellCard("S1", Faction.RESISTENZA_UMANA, List.of(scudo));
        EquipmentCard gear = new EquipmentCard("E1", Faction.RESISTENZA_UMANA, List.of(scudo));
        for (Card c : List.<Card>of(creature, spell, gear)) {
            assertEquals(List.of(scudo), c.abilities(), c.id());
        }
        assertTrue(card(1, 1).abilities().isEmpty());
        assertTrue(new SpellCard("S2", Faction.RESISTENZA_UMANA).abilities().isEmpty());
    }

    @Test
    void anAbilityIsAReferenceOrAWholeEffect() {
        ExpansionDef expansion = new ExpansionDef(
                "T0", Map.of(), "E28", Map.of(), "S1", Map.of(), "D_Permanente", List.of(), ResClass.C);
        List<Ability> abilities = List.of(new AbilityRef("K_SCUDO"), new InlineEffect(List.of(expansion)));
        SpellCard spell = new SpellCard("S1", Faction.POTENZE_ARTIFICIALI, abilities);
        assertEquals(abilities, spell.abilities(), "any card can have a keyword or a whole effect, in order");
    }

    @Test
    void abilityParamsKeepFileOrder() {
        Map<String, Value> params = new LinkedHashMap<>();
        params.put("z", new Value.IntV(1));
        params.put("a", new Value.IntV(2));
        assertEquals(List.of("z", "a"), List.copyOf(new AbilityRef("K_X", params).params().keySet()));
    }

    @Test
    void abilityValidation() {
        assertThrows(IllegalArgumentException.class, () -> new AbilityRef(" "));
        assertThrows(NullPointerException.class, () -> new AbilityRef(null));
        assertThrows(IllegalArgumentException.class, () -> new InlineEffect(List.of()));
    }

    @Test
    void cardsCopyTheirAbilityList() {
        List<Ability> list = new ArrayList<>(List.of(new AbilityRef("K_A")));
        CreatureCard c = new CreatureCard("x", 5, Suit.SPIRA, Subfaction.YLMOR, 1, 1, list);
        list.add(new AbilityRef("K_B"));
        assertEquals(1, c.abilities().size());
    }

    // ---- PlayerState ----

    @Test
    void creaturesAreListedByLaneAndIncludeDestroyedOnes() {
        PlayerState p = Boards.side(Subfaction.SEKTAC, 12000, "5:6V(1/1) 2:3F(1/1)! 3:9S(1/1)");
        assertEquals(List.of(2, 3, 5), p.creatures().stream().map(Creature::lane).toList());
    }

    @Test
    void targetableLanesSkipDestroyedCreaturesUnlessAskedTo() {
        PlayerState p = Boards.side(Subfaction.SEKTAC, 12000, "1:2V(1/1) 3:4S(1/1)! 4:5T(1/1)");
        assertArrayEquals(new boolean[] {true, false, false, true, false}, p.targetableLanes(false));
        assertArrayEquals(new boolean[] {true, false, true, true, false}, p.targetableLanes(true));
    }

    @Test
    void creaturesListIsASnapshot() {
        PlayerState p = Boards.side(Subfaction.SEKTAC, 12000, "1:2V(1/1)");
        assertThrows(UnsupportedOperationException.class, () -> p.creatures().clear());
    }

    @Test
    void placeAndRemove() {
        PlayerState p = Boards.side(Subfaction.SEKTAC, 12000, "");
        Creature c = new Creature(card(1, 1), 4);
        p.place(c);
        assertSame(c, p.creatureAt(4).orElseThrow());
        assertSame(c, p.remove(4));
        assertEquals(Optional.empty(), p.creatureAt(4));
    }

    @Test
    void aLaneHoldsOneCreature() {
        PlayerState p = Boards.side(Subfaction.SEKTAC, 12000, "4:2V(1/1)");
        assertThrows(IllegalStateException.class, () -> p.place(new Creature(card(1, 1), 4)));
        assertThrows(IllegalStateException.class, () -> p.remove(1));
    }

    @Test
    void lifeCanGoBelowZero() {
        PlayerState p = Boards.side(Subfaction.SEKTAC, 1000, "");
        p.loseLife(2750);
        assertEquals(-1750, p.life());
        assertThrows(IllegalArgumentException.class, () -> p.loseLife(-1));
    }

    @Test
    void theDeckIsCopiedAndIndexZeroIsTheTop() {
        List<Card> source = new ArrayList<>(Boards.deck(Subfaction.SEKTAC, "KS(1/1) 2F(1/1)"));
        PlayerState p = new PlayerState(Faction.POTENZE_ARTIFICIALI, 1, source);
        source.clear();
        assertEquals(List.of("KS", "2F"), Boards.ids(p.deck()));
        assertTrue(p.discard().isEmpty());
    }

    // ---- MatchState ----

    @Test
    void theTurnStartsAtOneAndGoesUp() {
        MatchState m = Boards.match(Boards.side(Subfaction.ADATTATI, 1, ""), Boards.side(Subfaction.SEKTAC, 1, ""));
        assertEquals(1, m.turn());
        m.nextTurn();
        assertEquals(2, m.turn());
    }

    @Test
    void turnFiftyIsTheLast() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 1, "");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 1, "");
        assertEquals(50, MatchState.LAST_TURN);
        MatchState m = new MatchState(a, b, 50);
        assertThrows(IllegalStateException.class, m::nextTurn);
        assertThrows(IllegalArgumentException.class, () -> new MatchState(a, b, 51));
        assertThrows(IllegalArgumentException.class, () -> new MatchState(a, b, 0));
    }

    @Test
    void playersAreFoundBySide() {
        PlayerState a = Boards.side(Subfaction.ADATTATI, 1, "");
        PlayerState b = Boards.side(Subfaction.SEKTAC, 2, "");
        MatchState m = new MatchState(a, b);
        assertSame(a, m.player(Side.A));
        assertSame(b, m.player(Side.B));
        assertEquals(Side.B, Side.A.other());
        assertEquals(Side.A, Side.B.other());
    }

    // ---- Attack: which cycle (ruleset 9.2) ----

    @Test
    void anAttackOnTheOppositeLaneIsInCycleOne() {
        Attack a = new Attack(Side.A, 3, Optional.of(new Target(Side.B, 3)));
        assertTrue(a.inCycle1());
        assertEquals(1, a.cycle());
    }

    @Test
    void anAttackOnAnotherLaneIsInCycleTwo() {
        Attack a = new Attack(Side.A, 4, Optional.of(new Target(Side.B, 1)));
        assertFalse(a.inCycle1());
        assertEquals(2, a.cycle());
    }

    @Test
    void anAttackWithoutATargetIsInCycleTwo() {
        Attack a = new Attack(Side.A, 2, Optional.empty());
        assertFalse(a.inCycle1());
        assertEquals(2, a.cycle());
    }

    @Test
    void anAttackAndItsTargetNeedARealLane() {
        assertThrows(IllegalArgumentException.class, () -> new Attack(Side.A, 0, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new Attack(Side.A, 6, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new Target(Side.B, 6));
    }
}
