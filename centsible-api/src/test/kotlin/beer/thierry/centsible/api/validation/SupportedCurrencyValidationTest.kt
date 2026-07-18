package beer.thierry.centsible.api.validation

import beer.thierry.centsible.api.model.budgetaccount.Currency
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SupportedCurrencyValidationTest {

    data class Holder(@field:SupportedCurrency val currency: String)
    data class NullableHolder(@field:SupportedCurrency val currency: String?)

    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

    private fun templates(currency: String): Set<String> =
        validator.validate(Holder(currency)).map { it.messageTemplate }.toSet()

    @Test
    fun `every Currency enum code is accepted`() {
        Currency.entries.forEach { assertTrue(templates(it.name).isEmpty(), "rejected ${it.name}") }
    }

    @Test
    fun `an unknown code is rejected`() {
        assertEquals(setOf("{validation.currency.unsupported}"), templates("XXX"))
    }

    @Test
    fun `a lowercase code is rejected, matching the old regex`() {
        assertEquals(setOf("{validation.currency.unsupported}"), templates("eur"))
    }

    @Test
    fun `null is left to a separate NotBlank`() {
        assertTrue(validator.validate(NullableHolder(null)).isEmpty())
    }
}
