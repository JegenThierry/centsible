package beer.thierry.centsible.api.model.auth

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class AuthRegisterRequest(
    @field:NotBlank(message = "{validation.username.required}")
    @field:Pattern(
        regexp = "^[a-zA-Z0-9_.-]{3,50}$",
        message = "{validation.username.pattern}"
    )
    var username: String = "",

    @field:NotBlank(message = "{validation.password.required}")
    @field:Size(max = 72, message = "{validation.password.tooLong}")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,}$",
        message = "{validation.password.strength}"
    )
    var password: String = "",

    @field:NotBlank(message = "{validation.email.required}")
    @field:Email(message = "{validation.email.invalid}")
    @field:Size(max = 255, message = "{validation.email.tooLong}")
    var email: String = "",

    @field:NotBlank(message = "{validation.firstName.required}")
    @field:Size(max = 100, message = "{validation.firstName.tooLong}")
    var firstName: String = "",

    @field:NotBlank(message = "{validation.lastName.required}")
    @field:Size(max = 100, message = "{validation.lastName.tooLong}")
    var lastName: String = "",

    /** Optional. Defaults to 'en' if omitted or unrecognised. Used for the confirmation email locale and the user's stored preference. */
    @field:Pattern(
        regexp = "^(en|fr|de)?$",
        message = "{validation.locale.unsupported}"
    )
    var locale: String? = null,
)
