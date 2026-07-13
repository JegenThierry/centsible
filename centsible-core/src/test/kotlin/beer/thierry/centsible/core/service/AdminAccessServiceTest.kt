package beer.thierry.centsible.core.service

import beer.thierry.centsible.core.services.admin.AdminAccessService
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AdminAccessServiceTest {

    @Test
    fun `nobody is admin while the feature is disabled`() {
        val access = AdminAccessService(adminEnabled = false, adminUsername = "admin")
        assertFalse(access.isEnabled())
        assertFalse(access.isAdmin("admin"))
    }

    @Test
    fun `nobody is admin while the username is blank`() {
        val access = AdminAccessService(adminEnabled = true, adminUsername = "")
        assertTrue(access.isEnabled())
        assertFalse(access.isAdmin(""))
        assertFalse(access.isAdmin("admin"))
    }

    @Test
    fun `only the exact configured username is admin`() {
        val access = AdminAccessService(adminEnabled = true, adminUsername = "admin")
        assertTrue(access.isAdmin("admin"))
        assertFalse(access.isAdmin("Admin"))
        assertFalse(access.isAdmin("someone-else"))
    }
}
