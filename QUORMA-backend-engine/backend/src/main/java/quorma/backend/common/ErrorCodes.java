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
    public static final String FACTION_UNKNOWN = "validation.faction.unknown";
    public static final String DECK_NAME_MAX_LENGTH = "validation.deck_name.max_length";

    public static final String USERNAME_TAKEN = "auth.username_taken";
    public static final String EMAIL_TAKEN = "auth.email_taken";
    public static final String CREDENTIALS_TAKEN = "auth.credentials_taken";
    public static final String INVALID_CREDENTIALS = "auth.invalid_credentials";

    public static final String PACK_UNKNOWN_TYPE = "pack.unknown_type";
    public static final String PACK_INSUFFICIENT_CREDITS = "pack.insufficient_credits";
    public static final String PACK_NO_POOL = "pack.no_pool";
    public static final String PACK_IDEMPOTENCY_KEY_INVALID = "pack.idempotency_key_invalid";
    public static final String PACK_IDEMPOTENCY_KEY_REUSED = "pack.idempotency_key_reused";

    public static final String DECK_NOT_FOUND = "deck.not_found";
    public static final String DECK_NAME_TAKEN = "deck.name_taken";
    public static final String DECK_DEFENSE_UNDELETABLE = "deck.defense_undeletable";
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

    public static final String ENGINE_INPUT_INVALID = "engine.input_invalid";
    public static final String ENGINE_UNAVAILABLE = "engine.unavailable";

    public static final String NOT_IMPLEMENTED = "error.not_implemented";

    private ErrorCodes() {
    }
}
