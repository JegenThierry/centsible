package beer.thierry.budgetplanner.api.service.users

import beer.thierry.budgetplanner.api.model.user.ProfileUpdateDTO
import beer.thierry.budgetplanner.api.model.user.UserDTO
import java.util.UUID

interface IUserService {
    fun fetchAllUsers(): List<UserDTO>
    fun fetchUserByUsername(username: String): UserDTO
    fun updateUserProfile(userId: UUID, profile: ProfileUpdateDTO): UserDTO
    fun updateProfilePicture(userId: UUID, profilePicture: String?): UserDTO
}
