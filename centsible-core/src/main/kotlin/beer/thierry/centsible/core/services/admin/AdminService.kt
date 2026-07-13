package beer.thierry.centsible.core.services.admin

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.admin.AdminUserDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IAdminRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.admin.IAdminAccessService
import beer.thierry.centsible.api.services.admin.IAdminService
import beer.thierry.centsible.api.services.authentication.IAuthService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AdminService(
    private val adminAccessService: IAdminAccessService,
    private val adminRepository: IAdminRepository,
    private val userRepository: IUserRepository,
    private val authService: IAuthService,
) : IAdminService {

    private val log = LoggerFactory.getLogger(AdminService::class.java)

    override fun listUsers(authenticatedUser: UserDTO): List<AdminUserDTO> {
        requireAdmin(authenticatedUser)
        return adminRepository.fetchAllUsers()
    }

    override fun deleteUser(authenticatedUser: UserDTO, userId: UUID) {
        requireAdmin(authenticatedUser)
        if (userId == authenticatedUser.id) {
            throw LocalizedException.BadRequest("error.admin.cannotDeleteSelf")
        }
        userRepository.findUserById(userId)
            ?: throw LocalizedException.NotFound("error.user.notFound")
        if (!userRepository.deleteUser(userId)) {
            throw LocalizedException.InternalError("error.user.deleteFailed")
        }
        log.info("Admin deleted user userId={} adminId={}", userId, authenticatedUser.id)
    }

    override fun resendVerificationEmail(authenticatedUser: UserDTO, userId: UUID) {
        requireAdmin(authenticatedUser)
        authService.resendRegistrationEmail(userId)
        log.info("Admin resent verification email userId={} adminId={}", userId, authenticatedUser.id)
    }

    private fun requireAdmin(authenticatedUser: UserDTO) {
        if (!adminAccessService.isEnabled()) {
            throw LocalizedException.Forbidden("error.admin.disabled")
        }
        if (!adminAccessService.isAdmin(authenticatedUser.username)) {
            log.warn("Admin access denied userId={}", authenticatedUser.id)
            throw LocalizedException.Forbidden("error.admin.forbidden")
        }
    }
}
