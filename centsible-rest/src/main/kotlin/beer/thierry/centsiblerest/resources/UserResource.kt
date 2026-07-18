package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.notification.NotificationSettingsDTO
import beer.thierry.centsible.api.model.user.DefaultCurrencyUpdateDTO
import beer.thierry.centsible.api.model.user.LocaleUpdateDTO
import beer.thierry.centsible.api.model.user.ProfileUpdateDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.users.IUserService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RequestMapping("/api/users")
@RestController
class UserResource(private val userService: IUserService) {

    private val log = LoggerFactory.getLogger(UserResource::class.java)

    @GetMapping("/myself")
    fun getUserByUsername(@AuthenticationPrincipal user: UserDTO): UserDTO =
        userService.fetchUserByUsername(user.username)

    @PutMapping("/profile")
    fun updateProfile(
        @AuthenticationPrincipal user: UserDTO,
        @Valid @RequestBody profile: ProfileUpdateDTO,
    ): UserDTO {
        val updated = userService.updateUserProfile(user.id, profile)
        log.info("Updated user profile userId={}", user.id)
        return updated
    }

    @PostMapping("/profile/picture")
    fun updateProfilePicture(
        @AuthenticationPrincipal user: UserDTO,
        @RequestParam("file") file: MultipartFile,
    ): UserDTO {
        val updated = userService.updateProfilePicture(user.id, file.toValidatedImageDataUrl())
        log.info("Updated user profile picture userId={} sizeBytes={}", user.id, file.size)
        return updated
    }

    @PutMapping("/locale")
    fun updateLocale(
        @AuthenticationPrincipal user: UserDTO,
        @Valid @RequestBody body: LocaleUpdateDTO,
    ): UserDTO {
        val updated = userService.updateUserLocale(user.id, body.locale)
        log.info("Updated user locale userId={} locale={}", user.id, body.locale)
        return updated
    }

    @PutMapping("/default-currency")
    fun updateDefaultCurrency(
        @AuthenticationPrincipal user: UserDTO,
        @Valid @RequestBody body: DefaultCurrencyUpdateDTO,
    ): UserDTO {
        val updated = userService.updateDefaultCurrency(user.id, body.defaultCurrency)
        log.info("Updated user default currency userId={} currency={}", user.id, body.defaultCurrency)
        return updated
    }

    @GetMapping("/notification-settings")
    fun fetchNotificationSettings(
        @AuthenticationPrincipal user: UserDTO,
    ): NotificationSettingsDTO =
        userService.fetchNotificationSettings(user.id)

    @PutMapping("/notification-settings")
    fun updateNotificationSettings(
        @AuthenticationPrincipal user: UserDTO,
        @RequestBody settings: NotificationSettingsDTO,
    ): NotificationSettingsDTO {
        val updated = userService.updateNotificationSettings(user.id, settings)
        log.info("Updated user notification settings userId={}", user.id)
        return updated
    }
}
