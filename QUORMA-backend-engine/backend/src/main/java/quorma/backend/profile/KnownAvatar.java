package quorma.backend.profile;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The value must be one of the codes of {@code profile.avatars}. A null is left to {@code @NotBlank}. */
@Documented
@Constraint(validatedBy = KnownAvatar.Validator.class)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface KnownAvatar {

    String message() default "validation.avatar.unknown";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<KnownAvatar, String> {

        private final ProfileProperties props;

        public Validator(ProfileProperties props) {
            this.props = props;
        }

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || props.isKnown(value);
        }
    }
}
