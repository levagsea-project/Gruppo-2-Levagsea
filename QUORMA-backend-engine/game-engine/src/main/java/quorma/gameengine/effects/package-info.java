/**
 * Card abilities at runtime:
 *
 * <ul>
 *   <li>{@link quorma.gameengine.effects.AbilityBinder}: the expansion parser;
 *   <li>{@link quorma.gameengine.effects.EffectRegistry}: the vocabulary the engine implements;
 *   <li>{@link quorma.gameengine.effects.EffectStack}: the order triggered effects resolve in;
 *   <li>{@link quorma.gameengine.effects.Effect}: what one effect does to its target, one class per vocabulary ID,
 *       found through {@link quorma.gameengine.effects.Effects}.
 * </ul>
 */
package quorma.gameengine.effects;
