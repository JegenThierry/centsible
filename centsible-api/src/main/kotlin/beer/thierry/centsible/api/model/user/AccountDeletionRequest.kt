package beer.thierry.centsible.api.model.user

import jakarta.validation.constraints.NotBlank

/**
 * Self-service account deletion. Always requires the account [password]; when the user has 2FA
 * enabled a current [totpCode] (TOTP or recovery code) is additionally required — both are
 * verified in the service. The deletion is irreversible and cascades to all of the user's data.
 */
data class AccountDeletionRequest(
    @field:NotBlank(message = "{validation.password.required}")
    val password: String = "",
    val totpCode: String? = null,
)
