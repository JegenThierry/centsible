package beer.thierry.centsible.api.model.user

import beer.thierry.centsible.api.validation.SupportedCurrency
import jakarta.validation.constraints.NotBlank

data class DefaultCurrencyUpdateDTO(
    @field:NotBlank(message = "{validation.currency.required}")
    @field:SupportedCurrency(message = "{validation.currency.unsupported}")
    var defaultCurrency: String = "EUR"
)
