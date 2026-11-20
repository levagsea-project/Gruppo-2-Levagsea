package quorma.gameengine.effects;

/**
 * An effect that, as a static one (trigger T0), changes the abilities of the creature it reaches: it grants a keyword
 * or an Evolution, or removes a keyword (E14, E15, E16). The same class resolves the effect when a trigger fires or a
 * spell is cast ({@link Effect}), so each of these effects is in one place.
 */
public interface AbilityLayerEffect extends EffectDefinition {

    /**
     * Applies the static effect {@code e}, which reaches the creature and whose conditions hold, to its abilities.
     * Static effects that grant or remove are read on the creature's own abilities, so a granted keyword never grants
     * another.
     */
    void layer(AbilityLayer layer, BoundExpansion e);
}
