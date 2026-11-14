package quorma.gameengine.testkit;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import quorma.gameengine.effects.AbilityBinder;
import quorma.gameengine.hand.Suit;
import quorma.gameengine.model.Ability;
import quorma.gameengine.model.Card;
import quorma.gameengine.model.Creature;
import quorma.gameengine.model.CreatureCard;
import quorma.gameengine.model.EquipmentCard;
import quorma.gameengine.model.MatchState;
import quorma.gameengine.model.PlayerState;
import quorma.gameengine.model.SpellCard;
import quorma.gameengine.model.Subfaction;

/**
 * Test helper: write a side of the table and its deck the way a player reads them.
 *
 * <pre>
 *   Boards.side(Subfaction.ADATTATI, 12000, "1:4V(4000/3000) 3:9F(1000/1500)")
 *   Boards.side(Subfaction.SEKTAC, 12000, "4V(4000/3000) 9F(1000/1500)", "KS(2000/2600) spell:S1")
 * </pre>
 *
 * <p><b>Board token</b>: {@code [lane:]RankSuit(ATK/DEF)[@SUBFACTION][!]}
 * <ul>
 *   <li>{@code lane}: 1..5. Without it, the lane is the position in the list (1, 2, 3...). Gaps are fine.
 *   <li>{@code Rank}: {@code A 2..10 J Q K} (Ace is 1). {@code Suit}: {@code V} Vincolo, {@code F} Frattura,
 *       {@code S} Spira, {@code T} Vestigio (V is taken, T as in "vesTigio"). Same letters as {@code Hands}.
 *   <li>{@code (ATK/DEF)}: base stats.
 *   <li>{@code @SUBFACTION}: overrides the side's base subfaction (same faction only), for example
 *       {@code @MEROZYN}. Needed to test the advantage matrix.
 *   <li>{@code !}: the creature is flagged destroyed (its DEF is unchanged).
 * </ul>
 * The card id of a board or deck creature is its rank and suit, for example {@code "10S"}.
 *
 * <p><b>Deck token</b>: {@code RankSuit(ATK/DEF)[@SUBFACTION]}, {@code spell:ID} or {@code equip:ID}. The first
 * token is the top of the deck (index 0). The faction of spells and equipment is the base subfaction's faction.
 */
public final class Boards {

    private static final String RANK = "A|J|Q|K|10|[2-9]";
    private static final Pattern BOARD_TOKEN = Pattern.compile(
            "^(?:([1-5]):)?(" + RANK + ")([VFST])\\((\\d+)/(\\d+)\\)(?:@([A-Za-z]+))?(!)?$");
    private static final Pattern DECK_TOKEN =
            Pattern.compile("^(" + RANK + ")([VFST])\\((\\d+)/(\\d+)\\)(?:@([A-Za-z]+))?$");

    private Boards() {}

    /** A side with no cards in its deck. */
    public static PlayerState side(Subfaction base, int life, String board) {
        return side(base, life, board, "");
    }

    public static PlayerState side(Subfaction base, int life, String board, String deck) {
        PlayerState player = new PlayerState(base.faction(), life, deck(base, deck));
        String[] tokens = tokens(board);
        for (int i = 0; i < tokens.length; i++) {
            Matcher m = BOARD_TOKEN.matcher(tokens[i]);
            if (!m.matches()) {
                throw new IllegalArgumentException("bad board token '" + tokens[i] + "': expected [lane:]RankSuit(ATK/DEF)[@SUB][!]");
            }
            int lane = m.group(1) != null ? Integer.parseInt(m.group(1)) : i + 1;
            Creature creature = new Creature(creatureCard(base, m.group(2), m.group(3), m.group(4), m.group(5), m.group(6)), lane);
            if (m.group(7) != null) {
                creature.markDestroyed();
            }
            player.place(creature);
        }
        return player;
    }

    /** A deck: the first token is the top. */
    public static List<Card> deck(Subfaction base, String spec) {
        List<Card> cards = new ArrayList<>();
        for (String token : tokens(spec)) {
            if (token.startsWith("spell:")) {
                cards.add(new SpellCard(token.substring("spell:".length()), base.faction()));
            } else if (token.startsWith("equip:")) {
                cards.add(new EquipmentCard(token.substring("equip:".length()), base.faction()));
            } else {
                Matcher m = DECK_TOKEN.matcher(token);
                if (!m.matches()) {
                    throw new IllegalArgumentException("bad deck token '" + token + "': expected RankSuit(ATK/DEF)[@SUB], spell:ID or equip:ID");
                }
                cards.add(creatureCard(base, m.group(1), m.group(2), m.group(3), m.group(4), m.group(5)));
            }
        }
        return cards;
    }

    /** A match at turn 1. */
    public static MatchState match(PlayerState a, PlayerState b) {
        return new MatchState(a, b);
    }

    public static MatchState match(PlayerState a, PlayerState b, int turn) {
        return new MatchState(a, b, turn);
    }

    /** The creature in a lane, which must exist. */
    public static Creature creature(PlayerState player, int lane) {
        return player.creatureAt(lane).orElseThrow(() -> new AssertionError("no creature in lane " + lane));
    }

    /**
     * Gives abilities to the creature in {@code lane}: replaces it with the same card plus {@code abilities}, bound
     * on the live ruleset as when a creature enters play. The creature must be undamaged and not destroyed.
     */
    public static Creature give(PlayerState player, int lane, Ability... abilities) {
        Creature old = creature(player, lane);
        CreatureCard c = old.card();
        if (old.destroyed() || old.currentDef() != c.def()) {
            throw new IllegalArgumentException("give abilities before the creature is damaged: " + old);
        }
        CreatureCard card = new CreatureCard(c.id(), c.rank(), c.suit(), c.subfaction(), c.atk(), c.def(), List.of(abilities));
        player.remove(lane);
        Creature fresh = new Creature(card, lane, new AbilityBinder(TurnResolvers.RULES).bind(card));
        player.place(fresh);
        return fresh;
    }

    /** Card ids, in list order. */
    public static List<String> ids(List<? extends Card> cards) {
        return cards.stream().map(Card::id).toList();
    }

    private static String[] tokens(String spec) {
        String trimmed = spec.trim();
        return trimmed.isEmpty() ? new String[0] : trimmed.split("\\s+");
    }

    private static CreatureCard creatureCard(
            Subfaction base, String rank, String suit, String atk, String def, String subfactionOverride) {
        Subfaction subfaction = base;
        if (subfactionOverride != null) {
            subfaction = Subfaction.valueOf(subfactionOverride.toUpperCase());
            if (subfaction.faction() != base.faction()) {
                throw new IllegalArgumentException(
                        "@" + subfactionOverride + " is not in the faction of " + base + ": a side has one faction");
            }
        }
        return new CreatureCard(
                rank + suit, rankValue(rank), suit(suit), subfaction, Integer.parseInt(atk), Integer.parseInt(def));
    }

    private static int rankValue(String rank) {
        return switch (rank) {
            case "A" -> 1;
            case "J" -> 11;
            case "Q" -> 12;
            case "K" -> 13;
            default -> Integer.parseInt(rank);
        };
    }

    private static Suit suit(String letter) {
        return switch (letter) {
            case "V" -> Suit.VINCOLO;
            case "F" -> Suit.FRATTURA;
            case "S" -> Suit.SPIRA;
            case "T" -> Suit.VESTIGIO;
            default -> throw new IllegalArgumentException("bad suit '" + letter + "'");
        };
    }
}
