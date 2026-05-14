package beer.thierry.budgetplanner.api.model.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ProfileUpdateDTO(
    @field:NotBlank(message = "First name is required.")
    @field:Size(max = 100, message = "First name must be at most 100 characters.")
    var firstName: String = "",

    @field:NotBlank(message = "Last name is required.")
    @field:Size(max = 100, message = "Last name must be at most 100 characters.")
    var lastName: String = "",

    @field:NotBlank(message = "Email is required.")
    @field:Email(message = "Email must be a valid address.")
    @field:Size(max = 255, message = "Email must be at most 255 characters.")
    var email: String = ""
)
