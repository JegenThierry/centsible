package beer.thierry.centsible.api.model.auth

import beer.thierry.centsible.api.validation.StrongPassword
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PasswordResetConfirmRequest(
    @field:NotBlank(message = "{validation.token.required}")
    @field:Size(max = 64, message = "{validation.token.invalid}")
    var token: String = "",

    @field:StrongPassword
    var password: String = "",
)
