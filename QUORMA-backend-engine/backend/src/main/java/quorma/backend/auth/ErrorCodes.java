package quorma.backend.auth;

/**
 * Error codes returned to clients instead of translated text. Each one is a key of {@code data/labels/<locale>.json},
 * where the frontend finds the message in the player's language. Never rename one without renaming it there too
 * ({@code data/validate.py} only checks the labels files, not this class).
 */
public final class ErrorCodes {

    public static final String VALIDATION_FAILED = "validation.failed";
    public static final String REQUIRED = "validation.required";
    public static final String USERNAME_PATTERN = "validation.username.pattern";
    public static final String EMAIL_INVALID = "validation.email.invalid";
    public static final String EMAIL_MAX_LENGTH = "validation.email.max_length";
    public static final String PASSWORD_SIZE = "validation.password.size";
    public static final String MAX_UTF8_BYTES = "validation.max_utf8_bytes";

    public static final String USERNAME_TAKEN = "auth.username_taken";
    public static final String EMAIL_TAKEN = "auth.email_taken";
    public static final String CREDENTIALS_TAKEN = "auth.credentials_taken";
    public static final String INVALID_CREDENTIALS = "auth.invalid_credentials";

    private ErrorCodes() {
    }
}
