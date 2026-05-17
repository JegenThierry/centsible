package beer.thierry.centsible.api.services.users

import beer.thierry.centsible.api.model.user.ProfileUpdateDTO
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.*

interface IUserService {
    fun fetchUserByUsername(username: String): UserDTO
    fun userExists(id: UUID): Boolean
    fun updateUserProfile(userId: UUID, profile: ProfileUpdateDTO): UserDTO
    fun updateProfilePicture(userId: UUID, profilePicture: String?): UserDTO
    fun updateUserLocale(userId: UUID, locale: String): UserDTO
}
