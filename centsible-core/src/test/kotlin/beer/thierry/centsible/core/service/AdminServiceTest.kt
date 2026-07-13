package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.admin.AdminUserDTO
import beer.thierry.centsible.api.model.user.User
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IAdminRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.admin.IAdminAccessService
import beer.thierry.centsible.api.services.authentication.IAuthService
import beer.thierry.centsible.core.services.admin.AdminService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class AdminServiceTest {

    @Mock
    private lateinit var adminAccessService: IAdminAccessService

    @Mock
    private lateinit var adminRepository: IAdminRepository

    @Mock
    private lateinit var userRepository: IUserRepository

    @Mock
    private lateinit var authService: IAuthService

    @InjectMocks
    private lateinit var service: AdminService

    private val admin = UserDTO(UUID.randomUUID(), "admin", "admin@x", "A", "D", "A D", null)

    private fun grantAdmin() {
        `when`(adminAccessService.isEnabled()).thenReturn(true)
        `when`(adminAccessService.isAdmin(admin.username)).thenReturn(true)
    }

    @Test
    fun `listUsers refuses when the admin feature is disabled`() {
        `when`(adminAccessService.isEnabled()).thenReturn(false)

        val ex = assertThrows(LocalizedException.Forbidden::class.java) { service.listUsers(admin) }
        assertEquals("error.admin.disabled", ex.messageKey)
        verify(adminRepository, never()).fetchAllUsers()
    }

    @Test
    fun `listUsers refuses a caller who is not the configured admin`() {
        `when`(adminAccessService.isEnabled()).thenReturn(true)
        `when`(adminAccessService.isAdmin(admin.username)).thenReturn(false)

        val ex = assertThrows(LocalizedException.Forbidden::class.java) { service.listUsers(admin) }
        assertEquals("error.admin.forbidden", ex.messageKey)
        verify(adminRepository, never()).fetchAllUsers()
    }

    @Test
    fun `listUsers returns the cross-user summaries for the admin`() {
        grantAdmin()
        val users = listOf(AdminUserDTO(username = "alice"), AdminUserDTO(username = "bob"))
        `when`(adminRepository.fetchAllUsers()).thenReturn(users)

        assertEquals(users, service.listUsers(admin))
    }

    @Test
    fun `deleteUser refuses to delete the admin's own account`() {
        grantAdmin()

        val ex = assertThrows(LocalizedException.BadRequest::class.java) {
            service.deleteUser(admin, admin.id)
        }
        assertEquals("error.admin.cannotDeleteSelf", ex.messageKey)
        verify(userRepository, never()).deleteUser(admin.id)
    }

    @Test
    fun `deleteUser throws not-found for an unknown user`() {
        grantAdmin()
        val target = UUID.randomUUID()
        `when`(userRepository.findUserById(target)).thenReturn(null)

        val ex = assertThrows(LocalizedException.NotFound::class.java) {
            service.deleteUser(admin, target)
        }
        assertEquals("error.user.notFound", ex.messageKey)
        verify(userRepository, never()).deleteUser(target)
    }

    @Test
    fun `deleteUser delegates to the repository for an existing user`() {
        grantAdmin()
        val target = UUID.randomUUID()
        `when`(userRepository.findUserById(target)).thenReturn(User(id = target, username = "alice"))
        `when`(userRepository.deleteUser(target)).thenReturn(true)

        service.deleteUser(admin, target)
        verify(userRepository).deleteUser(target)
    }

    @Test
    fun `resendVerificationEmail delegates to the authentication service`() {
        grantAdmin()
        val target = UUID.randomUUID()

        service.resendVerificationEmail(admin, target)
        verify(authService).resendRegistrationEmail(target)
    }

    @Test
    fun `resendVerificationEmail refuses a caller who is not the configured admin`() {
        `when`(adminAccessService.isEnabled()).thenReturn(true)
        `when`(adminAccessService.isAdmin(admin.username)).thenReturn(false)
        val target = UUID.randomUUID()

        assertThrows(LocalizedException.Forbidden::class.java) {
            service.resendVerificationEmail(admin, target)
        }
        verify(authService, never()).resendRegistrationEmail(target)
    }
}
