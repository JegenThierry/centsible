package beer.thierry.centsible.api.model.user

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class DefaultCurrencyUpdateDTO(
    @field:NotBlank(message = "{validation.currency.required}")
    @field:Pattern(
        regexp = "^(EUR|USD|JPY|GBP|AUD|CAD|CHF|CNY|HKD|NZD|SEK|NOK|DKK|SGD|KRW|INR|MXN|BRL|ZAR|TRY|PLN|PHP|IDR)$",
        message = "{validation.currency.unsupported}"
    )
    var defaultCurrency: String = "EUR"
)
