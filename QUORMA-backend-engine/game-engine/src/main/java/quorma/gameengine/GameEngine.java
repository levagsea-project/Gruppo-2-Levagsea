package quorma.gameengine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import quorma.gameengine.data.Ruleset;
import quorma.gameengine.data.RulesetLoader;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.effects.AbilityException;
import quorma.gameengine.input.MatchInput;
import quorma.gameengine.input.MatchInputException;
import quorma.gameengine.input.MatchInputReader;
import quorma.gameengine.log.BoardSnapshot;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.rng.Shuffle;
import quorma.gameengine.rng.SplitMix64;
import quorma.gameengine.turn.MatchReport;
import quorma.gameengine.turn.MatchRunner;
import quorma.gameengine.turn.Setup;
import quorma.gameengine.turn.TurnResolver;

/**
 * Plays a whole match from its input: same input, same match (Regola d'Oro 0).
 *
 * Thread-safe: a {@code GameEngine} holds only the immutable ruleset, every match has its own state.
 */
public final class GameEngine {

    private final Ruleset rules;
    private final String rulesetHash;

    /** An engine on the rule pack bundled in the jar. */
    public GameEngine() {
        this(RulesetLoader.fromClasspath(), RulesetLoader.rulesetHashFromClasspath());
    }

    /**
     * @param rulesetHash the hash of the rule pack {@code rules} was loaded from
     *                    ({@code RulesetLoader.rulesetHashFromDirectory} or {@code ...FromClasspath})
     */
    public GameEngine(Ruleset rules, String rulesetHash) {
        this.rules = Objects.requireNonNull(rules, "rules");
        this.rulesetHash = Objects.requireNonNull(rulesetHash, "rulesetHash");
    }

    public Ruleset rules() {
        return rules;
    }

    /** SHA-256 of the rule pack this engine plays, recorded in every MatchLog. */
    public String rulesetHash() {
        return rulesetHash;
    }

    /** Reads the input JSON, plays it and returns the canonical MatchLog JSON. */
    public String matchLog(String inputJson) {
        return simulate(inputJson).toMatchLog();
    }

    /** Reads the input JSON and plays it. */
    public Simulation simulate(String inputJson) {
        return simulate(MatchInputReader.read(inputJson));
    }

    /**
     * Plays the match of {@code input}.
     *
     * @throws MatchInputException if the input cannot be played on this ruleset
     */
    public Simulation simulate(MatchInput input) {
        AbilityBinder binder = new AbilityBinder(rules);
        check(input, binder);

        SplitMix64 rng = new SplitMix64(input.seed());
        Map<Side, List<Card>> decks = new EnumMap<>(Side.class);
        Map<Side, List<String>> drawOrder = new EnumMap<>(Side.class);
        for (Side side : Side.values()) {
            List<Card> deck = Shuffle.shuffled(input.player(side).deck(), rng);
            decks.put(side, deck);
            drawOrder.put(side, deck.stream().map(Card::id).toList());
        }

        int life = rules.startingLife();
        MatchState state = new MatchState(
                new PlayerState(input.player(Side.A).faction(), life, decks.get(Side.A)),
                new PlayerState(input.player(Side.B).faction(), life, decks.get(Side.B)));
        List<MatchEvent> setup = new ArrayList<>();
        Setup.run(state, binder, rng, setup);
        BoardSnapshot setupBoard = BoardSnapshot.of(state);

        MatchReport report = MatchRunner.run(state, new TurnResolver(rules));
        return new Simulation(EngineVersion.VERSION, rules.rulesetVersion(), rulesetHash, input, life, drawOrder, setup,
                setupBoard, report);
    }

    private void check(MatchInput input, AbilityBinder binder) {
        if (!input.rulesetVersion().equals(rules.rulesetVersion())) {
            throw new MatchInputException("input.ruleset_version: this engine plays " + rules.rulesetVersion()
                    + ", got " + input.rulesetVersion());
        }
        if (input.rulesetHash().isPresent() && !input.rulesetHash().get().equals(rulesetHash)) {
            throw new MatchInputException("input.ruleset_hash: this engine plays the rule pack " + rulesetHash
                    + ", got " + input.rulesetHash().get() + " (ruleset 12.6: a replay needs the same rules)");
        }
        for (Side side : Side.values()) {
            MatchInput.Player player = input.player(side);
            Set<String> ids = new HashSet<>();
            List<Card> deck = player.deck();
            for (int i = 0; i < deck.size(); i++) {
                Card card = deck.get(i);
                String path = "players." + side + ".deck[" + i + "]";
                if (!ids.add(card.id())) {
                    throw new MatchInputException(path + ".id: " + card.id() + " is already in this deck");
                }
                if (card.faction() != player.faction()) {
                    throw new MatchInputException(path + ": card " + card.id() + " is " + card.faction()
                            + ", the player is " + player.faction());
                }
                if (card instanceof CreatureCard creature) {
                    try {
                        binder.bind(creature);
                    } catch (AbilityException e) {
                        throw new MatchInputException(path + ".abilities: " + e.getMessage(), e);
                    }
                } else if (!card.abilities().isEmpty()) {
                    throw new MatchInputException(path + ".abilities: card " + card.id()
                            + ": spell and equipment effects are not implemented by the engine yet");
                }
            }
        }
    }
}
