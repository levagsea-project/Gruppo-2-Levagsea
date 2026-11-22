package quorma.gameengine.cards;

/** The codes of {@link CardCheckReport}: stable, language-neutral identifiers (the caller maps them to text). */
public final class CardCheckCodes {

    private CardCheckCodes() {}

    /** The card breaks the card schema (a missing or unknown field, a value out of range, a bad id). */
    public static final String CARD_INVALID = "card.invalid";
    /** The card belongs to the other faction (a creature's faction is its subfaction's). */
    public static final String CARD_WRONG_FACTION = "card.wrong_faction";
    /** The same keyword or evolution is listed twice on one card. */
    public static final String CARD_DUPLICATE_ABILITY = "card.duplicate_ability";
    /** More keywords than {@code balance.globals.card_ability_slots.keyword}. */
    public static final String CARD_TOO_MANY_KEYWORDS = "card.too_many_keywords";
    /** More evolutions than {@code balance.globals.card_ability_slots.evolution}. */
    public static final String CARD_TOO_MANY_EVOLUTIONS = "card.too_many_evolutions";
    /** A keyword or evolution on a rank that is not in its {@code allowed_ranks}. */
    public static final String CARD_RANK_NOT_ALLOWED = "card.rank_not_allowed";
    /** A spell or an equipment with no ability (only a creature may have none: a vanilla creature). */
    public static final String CARD_NO_ABILITY = "card.no_ability";
    /** The engine cannot bind an ability: unknown id, missing or unknown param, an effect that does not fit its target. */
    public static final String CARD_BIND_FAILED = "card.bind_failed";

    /** The request has no card at all. */
    public static final String POOL_EMPTY = "pool.empty";
    /** Two cards of the pool have the same id (the first one is kept and checked). */
    public static final String POOL_DUPLICATE_CARD_ID = "pool.duplicate_card_id";

    /** A pack pool names a card that is not in the pool. */
    public static final String PACK_UNKNOWN_CARD = "pack.unknown_card";
    /** A pack pool has slots of a Tier and no creature of that Tier. */
    public static final String PACK_NO_CREATURE_FOR_TIER = "pack.no_creature_for_tier";
    /** A pack pool has support slots and no spell or equipment. */
    public static final String PACK_NO_SUPPORT_CARD = "pack.no_support_card";
}
