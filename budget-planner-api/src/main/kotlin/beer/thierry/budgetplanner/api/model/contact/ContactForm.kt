package beer.thierry.budgetplanner.api.model.contact

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ContactForm(
    @field:NotBlank(message = "First name is required.")
    @field:Size(max = 100, message = "First name must be at most 100 characters.")
    var firstName: String = "",

    @field:Size(max = 100, message = "Last name must be at most 100 characters.")
    var lastName: String? = null,
)
