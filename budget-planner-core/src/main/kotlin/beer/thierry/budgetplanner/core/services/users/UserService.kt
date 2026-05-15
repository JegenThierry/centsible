package beer.thierry.budgetplanner.core.services.users

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
            ?: throw IllegalArgumentException("User with username $username not found")
        return mapToDTO(user)
    }

    override fun updateUserProfile(userId: UUID, profile: ProfileUpdateDTO): UserDTO {
        val user = userRepository.findUserById(userId)
            ?: throw IllegalArgumentException("User with ID $userId not found")

        val updatedUser = userRepository.updateUserProfile(
            id = userId,
            firstName = profile.firstName,
            lastName = profile.lastName,
            email = profile.email,
            profilePicture = user.profilePicture
        ) ?: throw IllegalStateException("Failed to update user profile")

        return mapToDTO(updatedUser)
    }

    override fun updateProfilePicture(userId: UUID, profilePicture: String?): UserDTO {
        val user = userRepository.findUserById(userId)
            ?: throw IllegalArgumentException("User with ID $userId not found")

        val updatedUser = userRepository.updateUserProfile(
            id = userId,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            profilePicture = profilePicture
        ) ?: throw IllegalStateException("Failed to update profile picture")

        return mapToDTO(updatedUser)
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
        )
    }
}
