package beer.thierry.centsible.api.model.contact

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ContactForm(
    @field:NotBlank(message = "{validation.firstName.required}")
    @field:Size(max = 100, message = "{validation.firstName.tooLong}")
    var firstName: String = "",

    @field:Size(max = 100, message = "{validation.lastName.tooLong}")
    var lastName: String? = null,
)
