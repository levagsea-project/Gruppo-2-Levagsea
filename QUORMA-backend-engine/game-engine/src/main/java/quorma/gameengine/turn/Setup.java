package quorma.gameengine.turn;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.log.MatchEvent;
import quorma.gameengine.log.MatchEvent.CardDrawn;
import quorma.gameengine.log.MatchEvent.CardSetAside;
import quorma.gameengine.log.MatchEvent.DeckReshuffled;
import quorma.gameengine.log.MatchEvent.LaneLeftEmpty;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.Side;
import quorma.gameengine.rng.Shuffle;
import quorma.gameengine.rng.SplitMix64;

/**
 * Setup (ruleset 5, decision {@code 2026-11-14-match-setup}), after both decks have been shuffled:
 *
 * <ol>
 *   <li>player A, then player B, fills lanes 1 to 5 from the top of the deck. A creature goes in the lane; a spell
 *       or equipment is set aside with no effect and the next card is drawn; when the deck is empty the lane stays
 *       empty;
 *   <li>player A, then player B, puts the set-aside cards at the bottom of the deck, in the order they were set
 *       aside, and shuffles the whole deck with the match PRNG. A player who set nothing aside does not shuffle,
 *       so the PRNG is not used.
 * </ol>
 *
 * Nothing triggers during setup: no live ability triggers on entering play.
 */
public final class Setup {

    private Setup() {}

    /**
     * @param rng the match PRNG, already used to shuffle A's deck then B's (ruleset 12.2)
     * @param log where setup appends its events
     */
    public static void run(MatchState state, AbilityBinder binder, SplitMix64 rng, List<MatchEvent> log) {
        Objects.requireNonNull(binder, "binder");
        Objects.requireNonNull(rng, "rng");
        Map<Side, List<Card>> setAside = Map.of(Side.A, new ArrayList<>(), Side.B, new ArrayList<>());
        for (Side side : Side.values()) {
            fillLanes(side, state.player(side), binder, setAside.get(side), log);
        }
        for (Side side : Side.values()) {
            List<Card> aside = setAside.get(side);
            if (aside.isEmpty()) {
                continue;
            }
            List<Card> deck = state.player(side).deck();
            deck.addAll(aside);
            Shuffle.fisherYates(deck, rng);
            log.add(new DeckReshuffled(side, aside.size(), deck.stream().map(Card::id).toList()));
        }
    }

    private static void fillLanes(Side side, PlayerState player, AbilityBinder binder, List<Card> aside, List<MatchEvent> log) {
        for (int lane = 1; lane <= PlayerState.LANES; lane++) {
            Creature placed = null;
            while (placed == null && !player.deck().isEmpty()) {
                Card card = player.deck().remove(0);
                if (card instanceof CreatureCard creature) {
                    placed = new Creature(creature, lane, binder.bind(creature));
                    player.place(placed);
                    log.add(new CardDrawn(side, lane, card.id()));
                } else {
                    aside.add(card);
                    log.add(new CardSetAside(side, card.id()));
                }
            }
            if (placed == null) {
                log.add(new LaneLeftEmpty(side, lane));
            }
        }
    }
}
