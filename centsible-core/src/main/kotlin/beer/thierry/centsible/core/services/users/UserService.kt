package beer.thierry.centsible.core.services.users

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.notification.NotificationSettingsDTO
import beer.thierry.centsible.api.model.user.ProfileUpdateDTO
import beer.thierry.centsible.api.model.user.User
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.users.IUserService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService(private val userRepository: IUserRepository) : IUserService {

    private val log = LoggerFactory.getLogger(UserService::class.java)

    override fun fetchUserByUsername(username: String): UserDTO {
        val user = userRepository.findUserByUsername(username)
            ?: throw LocalizedException.NotFound("error.user.notFoundByUsername", username)
        return mapToDTO(user)
    }

    override fun userExists(id: UUID): Boolean = userRepository.findUserById(id) != null

    override fun updateUserProfile(userId: UUID, profile: ProfileUpdateDTO): UserDTO {
        val dto = persistProfile(
            userId = userId,
            failureKey = "error.user.profileUpdateFailed",
        ) { existing ->
            existing.copy(
                firstName = profile.firstName,
                lastName = profile.lastName,
                email = profile.email,
            )
        }
        log.info("Updated user profile userId={}", userId)
        return dto
    }

    override fun updateProfilePicture(userId: UUID, profilePicture: String?): UserDTO {
        val dto = persistProfile(
            userId = userId,
            failureKey = "error.user.pictureUpdateFailed",
        ) { existing ->
            existing.copy(profilePicture = profilePicture)
        }
        log.info("Updated user profile picture userId={} cleared={}", userId, profilePicture == null)
        return dto
    }

    override fun updateUserLocale(userId: UUID, locale: String): UserDTO {
        val updated = userRepository.updateUserLocale(userId, locale)
            ?: throw LocalizedException.NotFound("error.user.notFound")
        log.info("Updated user locale userId={} locale={}", userId, locale)
        return mapToDTO(updated)
    }

    override fun fetchNotificationSettings(userId: UUID): NotificationSettingsDTO =
        userRepository.fetchNotificationSettings(userId)

    override fun updateNotificationSettings(
        userId: UUID,
        settings: NotificationSettingsDTO,
    ): NotificationSettingsDTO {
        val updated = userRepository.updateNotificationSettings(userId, settings)
        log.info("Updated notification settings userId={}", userId)
        return updated
    }

    private fun persistProfile(userId: UUID, failureKey: String, mutate: (User) -> User): UserDTO {
        val existing = userRepository.findUserById(userId)
            ?: throw LocalizedException.NotFound("error.user.notFound")
        val patched = mutate(existing)
        val updated = userRepository.updateUserProfile(
            id = userId,
            firstName = patched.firstName,
            lastName = patched.lastName,
            email = patched.email,
            profilePicture = patched.profilePicture,
        ) ?: throw LocalizedException.InternalError(failureKey)
        return mapToDTO(updated)
    }

    private fun mapToDTO(user: User): UserDTO = UserDTO(
        id = user.id,
        username = user.username,
        email = user.email,
        firstName = user.firstName,
        lastName = user.lastName,
        name = "${user.firstName} ${user.lastName}",
        profilePicture = user.profilePicture,
        locale = user.locale,
    )
}
