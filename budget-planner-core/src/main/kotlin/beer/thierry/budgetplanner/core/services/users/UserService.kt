package beer.thierry.budgetplanner.core.services.users

import beer.thierry.budgetplanner.api.exceptions.LocalizedException
import beer.thierry.budgetplanner.api.model.user.ProfileUpdateDTO
import beer.thierry.budgetplanner.api.model.user.User
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.repository.IUserRepository
import beer.thierry.budgetplanner.api.services.users.IUserService
import org.springframework.stereotype.Service
import java.util.*

@Service
class UserService(private val userRepository: IUserRepository) : IUserService {
    override fun fetchUserByUsername(username: String): UserDTO {
        val user = userRepository.findUserByUsername(username)
            ?: throw LocalizedException.NotFound("error.user.notFoundByUsername", username)
        return mapToDTO(user)
    }

    override fun userExists(id: UUID): Boolean = userRepository.findUserById(id) != null

    override fun updateUserProfile(userId: UUID, profile: ProfileUpdateDTO): UserDTO {
        val user = userRepository.findUserById(userId)
            ?: throw LocalizedException.NotFound("error.user.notFound")

        val updatedUser = userRepository.updateUserProfile(
            id = userId,
            firstName = profile.firstName,
            lastName = profile.lastName,
            email = profile.email,
            profilePicture = user.profilePicture
        ) ?: throw LocalizedException.InternalError("error.user.profileUpdateFailed")

        return mapToDTO(updatedUser)
    }

    override fun updateProfilePicture(userId: UUID, profilePicture: String?): UserDTO {
        val user = userRepository.findUserById(userId)
            ?: throw LocalizedException.NotFound("error.user.notFound")

        val updatedUser = userRepository.updateUserProfile(
            id = userId,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            profilePicture = profilePicture
        ) ?: throw LocalizedException.InternalError("error.user.pictureUpdateFailed")

        return mapToDTO(updatedUser)
    }

    override fun updateUserLocale(userId: UUID, locale: String): UserDTO {
        val updated = userRepository.updateUserLocale(userId, locale)
            ?: throw LocalizedException.NotFound("error.user.notFound")
        return mapToDTO(updated)
    }

    private fun mapToDTO(user: User): UserDTO {
        return UserDTO(
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
}
