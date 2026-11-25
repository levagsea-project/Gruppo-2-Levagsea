package quorma.backend.common;

/**
 * Error codes returned to clients instead of translated text. Each one is a key of {@code data/labels/<locale>.json},
 * where the frontend finds the message in the player's language. Never rename one without renaming it there too
 * ({@code data/validate.py} only checks the labels files, not this class).
 *
 * <p>One class for every domain, because label keys are one shared namespace. Add new codes in the section of their
 * domain, and the key to both {@code it.json} and {@code en.json} in the same commit.
 */
public final class ErrorCodes {

    public static final String VALIDATION_FAILED = "validation.failed";
    public static final String REQUIRED = "validation.required";
    public static final String USERNAME_PATTERN = "validation.username.pattern";
    public static final String EMAIL_INVALID = "validation.email.invalid";
    public static final String EMAIL_MAX_LENGTH = "validation.email.max_length";
    public static final String PASSWORD_SIZE = "validation.password.size";
    public static final String MAX_UTF8_BYTES = "validation.max_utf8_bytes";
    public static final String DISPLAY_NAME_PATTERN = "validation.display_name.pattern";
    public static final String AVATAR_UNKNOWN = "validation.avatar.unknown";
    public static final String FACTION_UNKNOWN = "validation.faction.unknown";
    public static final String DECK_NAME_MAX_LENGTH = "validation.deck_name.max_length";

    public static final String USERNAME_TAKEN = "auth.username_taken";
    public static final String EMAIL_TAKEN = "auth.email_taken";
    public static final String CREDENTIALS_TAKEN = "auth.credentials_taken";
    public static final String CURRENT_PASSWORD_WRONG = "auth.current_password_wrong";
    public static final String INVALID_CREDENTIALS = "auth.invalid_credentials";

    public static final String PACK_UNKNOWN_TYPE = "pack.unknown_type";
    public static final String PACK_INSUFFICIENT_CREDITS = "pack.insufficient_credits";
    public static final String PACK_NO_POOL = "pack.no_pool";
    public static final String PACK_NOT_ON_SALE = "pack.not_on_sale";
    public static final String PACK_IDEMPOTENCY_KEY_INVALID = "pack.idempotency_key_invalid";
    public static final String PACK_IDEMPOTENCY_KEY_REUSED = "pack.idempotency_key_reused";

    public static final String DECK_NOT_FOUND = "deck.not_found";
    public static final String DECK_NAME_TAKEN = "deck.name_taken";
    public static final String DECK_DEFENSE_UNDELETABLE = "deck.defense_undeletable";
    public static final String DECK_OUTDATED = "deck.outdated";
    public static final String DECK_DEFENSE_REQUIRED = "deck.defense_required";
    public static final String DECK_CONFLICT = "deck.conflict";
    public static final String DECK_INVALID = "deck.invalid";
    public static final String DECK_SIZE = "deck.size";
    public static final String DECK_CREATURE_COUNT = "deck.creature_count";
    public static final String DECK_DUPLICATE_SLOT = "deck.duplicate_slot";
    public static final String DECK_INSTANCE_DUPLICATED = "deck.instance_duplicated";
    public static final String DECK_INSTANCE_NOT_FOUND = "deck.instance_not_found";
    public static final String DECK_SNAPSHOT_INVALID = "deck.snapshot_invalid";
    public static final String DECK_CARD_WRONG_FACTION = "deck.card_wrong_faction";
    public static final String DECK_CARD_NO_DEFINITION = "deck.card_no_definition";

    public static final String CATALOGUE_DRAFT_NOT_FOUND = "catalogue.draft_not_found";
    public static final String CATALOGUE_DRAFT_OPEN_EXISTS = "catalogue.draft_open_exists";
    public static final String CATALOGUE_DRAFT_CLOSED = "catalogue.draft_closed";
    public static final String CATALOGUE_DRAFT_STALE = "catalogue.draft_stale";
    public static final String CATALOGUE_DRAFT_INVALID = "catalogue.draft_invalid";
    public static final String CATALOGUE_ITEM_INVALID = "catalogue.item_invalid";
    public static final String CATALOGUE_IMPORT_INVALID = "catalogue.import_invalid";
    public static final String CATALOGUE_IMPORT_TOO_LARGE = "catalogue.import_too_large";
    public static final String CATALOGUE_CARD_NOT_FOUND = "catalogue.card_not_found";
    public static final String CATALOGUE_SET_NOT_FOUND = "catalogue.set_not_found";
    public static final String CATALOGUE_SET_INVALID = "catalogue.set_invalid";
    public static final String CATALOGUE_TEXT_INVALID = "catalogue.text_invalid";
    public static final String CATALOGUE_TEXT_NAME_TAKEN = "catalogue.text_name_taken";
    public static final String CATALOGUE_PACK_TYPE_NOT_FOUND = "catalogue.pack_type_not_found";
    public static final String CATALOGUE_PACK_TYPE_INVALID = "catalogue.pack_type_invalid";
    public static final String CATALOGUE_PACK_TYPE_CONFLICT = "catalogue.pack_type_conflict";

    public static final String REPLAY_ENGINE_VERSION_MISMATCH = "replay.engine_version_mismatch";
    public static final String REPLAY_RULESET_MISMATCH = "replay.ruleset_mismatch";
    public static final String REPLAY_RESULT_MISMATCH = "replay.result_mismatch";

    public static final String CHALLENGE_NOT_FOUND = "challenge.not_found";
    public static final String CHALLENGE_NOT_PENDING = "challenge.not_pending";
    public static final String CHALLENGE_EXPIRED = "challenge.expired";
    public static final String CHALLENGE_SELF = "challenge.self";
    public static final String CHALLENGE_OPPONENT_NOT_FOUND = "challenge.opponent_not_found";
    public static final String CHALLENGE_ALREADY_PENDING = "challenge.already_pending";
    public static final String CHALLENGE_DECKS_INVALID = "challenge.decks_invalid";

    public static final String ROOM_NOT_FOUND = "room.not_found";
    public static final String ROOM_NOT_OPEN = "room.not_open";
    public static final String ROOM_EXPIRED = "room.expired";
    public static final String ROOM_OWN_ROOM = "room.own_room";
    public static final String ROOM_ALREADY_OPEN = "room.already_open";

    public static final String MATCHMAKING_NO_OPPONENT = "matchmaking.no_opponent";
    public static final String MATCH_NOT_FOUND = "match.not_found";

    public static final String USER_NOT_FOUND = "user.not_found";

    public static final String ENGINE_INPUT_INVALID = "engine.input_invalid";
    public static final String ENGINE_UNAVAILABLE = "engine.unavailable";

    public static final String NOT_IMPLEMENTED = "error.not_implemented";

    private ErrorCodes() {
    }
}
