package quorma.gameengine.data;

/**
 * One subfaction of {@code data/subfactions.json}: which faction it belongs to and its two relations.
 * Names are those of {@code vocab.constants.subfactions}, for example {@code "Adattati"}.
 *
 * @param name          subfaction name, the key in the file
 * @param faction       {@code "Resistenza Umana"} or {@code "Potenze Artificiali"}
 * @param cycle         cycle id ({@code MONDO} or {@code IDENTITA}), metadata only
 * @param strongAgainst the subfaction this one has the advantage against
 * @param weakAgainst   the subfaction this one has the disadvantage against
 */
public record SubfactionDef(String name, String faction, String cycle, String strongAgainst, String weakAgainst) {}
