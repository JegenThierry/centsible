package beer.thierry.budgetplanner.api.model.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class LocaleUpdateDTO(
    @field:NotBlank(message = "{validation.locale.required}")
    @field:Pattern(
        regexp = "^(en|fr|de)$",
        message = "{validation.locale.unsupported}"
    )
    var locale: String = "en"
)
