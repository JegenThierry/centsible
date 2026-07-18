package beer.thierry.centsible.api.model.user

import jakarta.validation.constraints.NotBlank

data class AccountDeletionRequest(
    @field:NotBlank(message = "{validation.password.required}")
    val password: String = "",
    val totpCode: String? = null,
)
