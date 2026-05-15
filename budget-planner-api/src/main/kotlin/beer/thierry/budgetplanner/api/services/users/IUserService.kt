package beer.thierry.budgetplanner.api.services.users

import beer.thierry.budgetplanner.api.model.user.ProfileUpdateDTO
import beer.thierry.budgetplanner.api.model.user.UserDTO
import java.util.*

interface IUserService {
    fun fetchUserByUsername(username: String): UserDTO
    fun userExists(id: UUID): Boolean
    fun updateUserProfile(userId: UUID, profile: ProfileUpdateDTO): UserDTO
    fun updateProfilePicture(userId: UUID, profilePicture: String?): UserDTO
}
