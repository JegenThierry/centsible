package beer.thierry.centsible.api.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import kotlin.reflect.KClass

/**
 * Composed password-strength constraint: non-blank, at most 72 characters (the bcrypt input limit),
 * and matching the shared complexity pattern (lower + upper + digit + special, length ≥ 8). Bundles
 * the three message keys that every password field used identically, so registration and password
 * reset can no longer drift apart.
 *
 * No `@ReportAsSingleViolation`: each failing component still reports its own message, exactly as the
 * three separate field annotations did before.
 */
@MustBeDocumented
@NotBlank(message = "{validation.password.required}")
@Size(max = 72, message = "{validation.password.tooLong}")
@Pattern(
    regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
    message = "{validation.password.strength}",
)
@Constraint(validatedBy = [])
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class StrongPassword(
    val message: String = "{validation.password.strength}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)
