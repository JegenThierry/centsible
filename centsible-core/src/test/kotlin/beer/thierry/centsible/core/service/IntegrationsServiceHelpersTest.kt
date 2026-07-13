package beer.thierry.centsible.core.services.integrations

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class IntegrationsServiceHelpersTest {

    @Test
    fun `syntheticUser carries only the id field`() {
        val id = UUID.randomUUID()
        val user = syntheticUser(id)
        assertEquals(id, user.id)
    }
}
