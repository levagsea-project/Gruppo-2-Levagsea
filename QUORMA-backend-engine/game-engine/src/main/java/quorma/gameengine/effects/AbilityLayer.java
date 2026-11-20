package quorma.gameengine.effects;

import java.util.List;

/**
 * The abilities of one creature as static effects change them: what the turn's {@code Board} builds when it adds the
 * keywords and Evolutions that static effects grant a creature and takes away the keywords they remove. An
 * {@link AbilityLayerEffect} edits it.
 */
public interface AbilityLayer {

    /** The binder that turns a keyword or an Evolution into expansions. */
    AbilityBinder binder();

    /** True if the creature has the ability {@code abilityId} now (its own, or one already added). */
    boolean has(String abilityId);

    /** Adds the expansions of a granted keyword or Evolution. */
    void add(List<BoundExpansion> expansions);

    /** Takes the keyword {@code keywordId} away. */
    void remove(String keywordId);
}
