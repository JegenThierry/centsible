package beer.thierry.centsible.api.model.auth

import jakarta.validation.constraints.NotBlank

data class PasswordChangeRequest(
    @field:NotBlank(message = "{validation.password.required}")
    val currentPassword: String = "",
    @field:NotBlank(message = "{validation.password.required}")
    val newPassword: String = "",
)
