package beer.thierry.centsible.api.model.auth

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class PasswordResetConfirmRequest(
    @field:NotBlank(message = "{validation.token.required}")
    @field:Size(max = 64, message = "{validation.token.invalid}")
    var token: String = "",

    @field:NotBlank(message = "{validation.password.required}")
    @field:Size(max = 72, message = "{validation.password.tooLong}")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
        message = "{validation.password.strength}"
    )
    var password: String = "",
)
