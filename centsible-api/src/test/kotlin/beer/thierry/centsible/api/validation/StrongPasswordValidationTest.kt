package beer.thierry.centsible.api.validation

import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StrongPasswordValidationTest {

    data class Holder(@field:StrongPassword val password: String)

    private val validator: Validator = Validation.buildDefaultValidatorFactory().validator

    private fun templates(password: String): Set<String> =
        validator.validate(Holder(password)).map { it.messageTemplate }.toSet()

    @Test
    fun `a strong password passes`() {
        assertTrue(templates("Aa1!aaaa").isEmpty())
    }

    @Test
    fun `blank fails the not-blank and strength rules`() {
        assertEquals(
            setOf("{validation.password.required}", "{validation.password.strength}"),
            templates(""),
        )
    }

    @Test
    fun `missing a character class fails only the strength rule`() {
        assertEquals(setOf("{validation.password.strength}"), templates("alllowercase1!"))
    }

    @Test
    fun `over 72 characters fails only the size rule`() {
        val long = "Aa1!" + "a".repeat(70)
        assertEquals(setOf("{validation.password.tooLong}"), templates(long))
    }
}
