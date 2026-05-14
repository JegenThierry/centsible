package beer.thierry.budgetplanner.api.model.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class AuthRegisterRequest(
    @field:NotBlank(message = "Username is required.")
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_.-]{3,50}$",
        message = "Username must be 3-50 characters and contain only letters, digits, '.', '_' or '-'."
    )
    var username: String = "",

    @field:NotBlank(message = "Password is required.")
    @field:Size(max = 72, message = "Password must be at most 72 characters.")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
        message = "Password must be 8+ characters with upper and lower case letters, a digit, and one of @\$!%*?&."
    )
    var password: String = "",

    @field:NotBlank(message = "Email is required.")
    @field:Email(message = "Email must be a valid address.")
    @field:Size(max = 255, message = "Email must be at most 255 characters.")
    var email: String = "",

    @field:NotBlank(message = "First name is required.")
    @field:Size(max = 100, message = "First name must be at most 100 characters.")
    var firstName: String = "",

    @field:NotBlank(message = "Last name is required.")
    @field:Size(max = 100, message = "Last name must be at most 100 characters.")
    var lastName: String = "",
)
