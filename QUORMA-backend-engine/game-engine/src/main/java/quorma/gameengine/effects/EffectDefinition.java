package quorma.gameengine.effects;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import quorma.gameengine.data.ResClass;
import quorma.gameengine.data.Value;

/**
 * What the engine says about one effect of the vocabulary (E…), whatever it does: its ID, the resolution classes it is
 * implemented for and the params it takes. {@link EffectRegistry} checks every card against these when it is bound.
 * Effects that resolve on a creature are an {@link Effect}, those that act on a player, the deck or a lane a
 * {@link PlayerEffect}; the others (static, replacement and a few triggered ones) are only a definition, because the
 * turn code reads them where they matter ({@code Board}, {@code Strike}, {@code Combat}, ...).
 */
public interface EffectDefinition {

    /**
     * The parts of an expansion an effect may have rules about.
     *
     * @param origin   where the expansion is written
     * @param cls      the resolution class
     * @param targetId the target ID
     * @param mode     the {@code mode} param, {@code ""} if the card has none
     * @param params   the effect params as written on the card
     */
    record Shape(Origin origin, ResClass cls, String targetId, String mode, Map<String, Value> params) {}

    /** The vocabulary ID this class implements, such as {@code "E8"}. Never renumbered. */
    String id();

    /** The resolution classes the effect is implemented for (the card's ability must be one of them). */
    Set<ResClass> classes();

    /**
     * The params the effect takes, per {@code mode} ({@code ""} when it has no mode). {@link EffectRegistry} checks a
     * card against these when it is bound.
     */
    Map<String, ParamRule> modes();

    /**
     * Checks beyond what {@link #modes()} says, for the effect's own rules. Returns the problem, empty if none. The
     * default has none.
     *
     * @param shape the expansion as the card writes it
     */
    default Optional<String> validate(Shape shape) {
        return Optional.empty();
    }
}
