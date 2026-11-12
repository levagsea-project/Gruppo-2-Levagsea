package quorma.backend.pack;

/**
 * Error codes of the pack endpoints.
*/

//TODO(Mattia): move error codes to a common place. Add them to language json files for translation.
public final class PackErrorCodes {

    public static final String REQUIRED = "validation.required";
    public static final String FACTION_UNKNOWN = "validation.faction.unknown";

    public static final String UNKNOWN_TYPE = "pack.unknown_type";
    public static final String INSUFFICIENT_CREDITS = "pack.insufficient_credits";
    public static final String NO_POOL = "pack.no_pool";
    public static final String IDEMPOTENCY_KEY_INVALID = "pack.idempotency_key_invalid";
    public static final String IDEMPOTENCY_KEY_REUSED = "pack.idempotency_key_reused";

    public static final String NOT_IMPLEMENTED = "error.not_implemented";

    private PackErrorCodes() {
    }
}
