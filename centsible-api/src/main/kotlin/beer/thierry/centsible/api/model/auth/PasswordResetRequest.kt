package beer.thierry.centsible.api.model.auth

import jakarta.validation.constraints.NotBlank

data class PasswordResetRequest(
    @field:NotBlank(message = "{validation.username.required}")
    var username: String = "",
)
