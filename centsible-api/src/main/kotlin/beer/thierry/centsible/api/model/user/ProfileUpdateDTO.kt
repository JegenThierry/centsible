package beer.thierry.centsible.api.model.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ProfileUpdateDTO(
    @field:NotBlank(message = "{validation.firstName.required}")
    @field:Size(max = 100, message = "{validation.firstName.tooLong}")
    var firstName: String = "",

    @field:NotBlank(message = "{validation.lastName.required}")
    @field:Size(max = 100, message = "{validation.lastName.tooLong}")
    var lastName: String = "",

    @field:NotBlank(message = "{validation.email.required}")
    @field:Email(message = "{validation.email.invalid}")
    @field:Size(max = 255, message = "{validation.email.tooLong}")
    var email: String = ""
)
