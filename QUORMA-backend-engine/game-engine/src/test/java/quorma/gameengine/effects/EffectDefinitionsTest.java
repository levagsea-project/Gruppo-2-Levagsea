package quorma.gameengine.effects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import quorma.gameengine.data.ResClass;

/** Every effect of the vocabulary the engine implements has one definition, and the registry reads them. */
class EffectDefinitionsTest {

    @Test
    void theEngineImplements45EffectsAndEachHasOneDefinition() {
        List<String> ids = EffectDefinitions.all().stream().map(EffectDefinition::id).toList();
        assertEquals(45, ids.size());
        assertEquals(45, Set.copyOf(ids).size(), "no ID twice");
        assertTrue(EffectDefinitions.find("E99").isEmpty());
    }

    @Test
    void theDefinitionsComeInTheOrderOfTheIdNumbers() {
        List<Integer> numbers = EffectDefinitions.all().stream()
                .map(d -> Integer.parseInt(d.id().substring(1))).toList();
        assertEquals(numbers.stream().sorted().toList(), numbers);
    }

    @Test
    void everyDefinitionNamesItsClassesAndItsParams() {
        for (EffectDefinition d : EffectDefinitions.all()) {
            assertFalse(d.classes().isEmpty(), d.id());
            assertFalse(d.modes().isEmpty(), d.id());
        }
    }

    @Test
    void theClassesAndTheIdsOfTheNamedConstantsAgree() {
        assertEquals(Optional.of(ResClass.C), EffectDefinitions.find(NoBonusEffect.ID).map(d -> d.classes().iterator().next()));
        assertEquals(Set.of(ResClass.R), EffectDefinitions.find(PreventAttackEffect.ID).orElseThrow().classes());
        assertEquals(Set.of(ResClass.T), EffectDefinitions.find(RedirectAttackEffect.ID).orElseThrow().classes());
        assertEquals("E61", NoBonusEffect.ID);
        assertEquals("E1", AtkEffect.ID);
    }

    @Test
    void definitionsOnlyCoverEffectsThatNeitherResolveOnACreatureNorOnAPlayer() {
        for (EffectDefinition d : EffectDefinitions.all()) {
            boolean resolves = d instanceof Effect || d instanceof PlayerEffect;
            assertEquals(Effects.find(d.id()).isPresent() || PlayerEffects.find(d.id()).isPresent(), resolves, d.id());
        }
    }
}
