package beer.thierry.budgetplanner.api.model.auth

import jakarta.validation.constraints.NotBlank

data class AuthRequest(
    @field:NotBlank(message = "Username is required.")
    var username: String = "",

    @field:NotBlank(message = "Password is required.")
    var password: String = "",
)
