package beer.thierry.centsible.api.services.users

import beer.thierry.centsible.api.model.notification.NotificationSettingsDTO
import beer.thierry.centsible.api.model.user.ProfileUpdateDTO
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.*

interface IUserService {
    fun fetchUserByUsername(username: String): UserDTO
    fun userExists(id: UUID): Boolean

    /** Current token version for [id] (for JWT revocation checks), or null when the user doesn't exist. */
    fun currentTokenVersion(id: UUID): Int?

    /** Records that [id] made an authenticated request (last_seen_at, throttled at the repository). */
    fun recordUserActivity(id: UUID)
    fun updateUserProfile(userId: UUID, profile: ProfileUpdateDTO): UserDTO
    fun updateProfilePicture(userId: UUID, profilePicture: String?): UserDTO
    fun updateUserLocale(userId: UUID, locale: String): UserDTO
    fun updateDefaultCurrency(userId: UUID, currency: String): UserDTO

    fun fetchNotificationSettings(userId: UUID): NotificationSettingsDTO
    fun updateNotificationSettings(userId: UUID, settings: NotificationSettingsDTO): NotificationSettingsDTO
}
