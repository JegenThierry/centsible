package beer.thierry.centsible.api.model.auth

import jakarta.validation.constraints.NotBlank

data class AuthRequest(
    @field:NotBlank(message = "{validation.username.required}")
    var username: String = "",

    @field:NotBlank(message = "{validation.password.required}")
    var password: String = "",
)
