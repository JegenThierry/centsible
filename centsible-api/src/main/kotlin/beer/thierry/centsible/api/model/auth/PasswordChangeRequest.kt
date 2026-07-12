package beer.thierry.centsible.api.model.auth

import jakarta.validation.constraints.NotBlank

/**
 * Authenticated password change: the caller proves they hold the [currentPassword] before the
 * [newPassword] replaces it. The new password's strength is enforced in the service (same rules
 * as registration/reset), so it is intentionally not pattern-validated here.
 */
data class PasswordChangeRequest(
    @field:NotBlank(message = "{validation.password.required}")
    val currentPassword: String = "",
    @field:NotBlank(message = "{validation.password.required}")
    val newPassword: String = "",
)
