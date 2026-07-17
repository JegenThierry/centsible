package beer.thierry.centsiblerest

import beer.thierry.centsible.api.model.auth.PasswordChangeRequest
import beer.thierry.centsible.api.model.auth.TotpCodeRequest
import beer.thierry.centsible.api.model.user.AccountDeletionRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper

/**
 * Spring Boot 4 binds @RequestBody with Jackson 3, discovering modules from the classpath. Without
 * `tools.jackson.module:jackson-module-kotlin` it cannot use Kotlin's primary constructor and falls
 * back to the synthetic no-arg one, leaving `val` properties at their defaults — which silently
 * blanked every 2FA code. Mirrors that discovery so dropping the dependency fails here, not in prod.
 */
class JacksonKotlinBindingTest {

    private val mapper = JsonMapper.builder().findAndAddModules().build()

    @Test
    fun `val constructor properties bind from the request body`() {
        assertEquals("123456", mapper.readValue("""{"code":"123456"}""", TotpCodeRequest::class.java).code)
        assertEquals("654321", mapper.readValue("""{"code":"654321"}""", TotpCodeRequest::class.java).code)
    }

    @Test
    fun `val constructor properties bind on the other affected payloads`() {
        val pw = mapper.readValue(
            """{"currentPassword":"old","newPassword":"new"}""",
            PasswordChangeRequest::class.java,
        )
        assertEquals("old", pw.currentPassword)
        assertEquals("new", pw.newPassword)

        val del = mapper.readValue("""{"password":"pw","totpCode":"123456"}""", AccountDeletionRequest::class.java)
        assertEquals("pw", del.password)
        assertEquals("123456", del.totpCode)
    }
}
