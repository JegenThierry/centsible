package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.notification.NotificationSettingsDTO
import beer.thierry.centsible.api.model.user.LocaleUpdateDTO
import beer.thierry.centsible.api.model.user.ProfileUpdateDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.users.IUserService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RequestMapping("/api/users")
@RestController
class UserResource(private val userService: IUserService) {

    private val log = LoggerFactory.getLogger(UserResource::class.java)

    @GetMapping("/myself")
    fun getUserByUsername(@AuthenticationPrincipal user: UserDTO): ResponseEntity<UserDTO> =
        ResponseEntity.ok(userService.fetchUserByUsername(user.username))

    @PutMapping("/profile")
    fun updateProfile(
        @AuthenticationPrincipal user: UserDTO,
        @Valid @RequestBody profile: ProfileUpdateDTO,
    ): ResponseEntity<UserDTO> {
        val updated = userService.updateUserProfile(user.id, profile)
        log.info("Updated user profile userId={}", user.id)
        return ResponseEntity.ok(updated)
    }

    @PostMapping("/profile/picture")
    fun updateProfilePicture(
        @AuthenticationPrincipal user: UserDTO,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<UserDTO> {
        val updated = userService.updateProfilePicture(user.id, file.toValidatedImageDataUrl())
        log.info("Updated user profile picture userId={} sizeBytes={}", user.id, file.size)
        return ResponseEntity.ok(updated)
    }

    @PutMapping("/locale")
    fun updateLocale(
        @AuthenticationPrincipal user: UserDTO,
        @Valid @RequestBody body: LocaleUpdateDTO,
    ): ResponseEntity<UserDTO> {
        val updated = userService.updateUserLocale(user.id, body.locale)
        log.info("Updated user locale userId={} locale={}", user.id, body.locale)
        return ResponseEntity.ok(updated)
    }

    @GetMapping("/notification-settings")
    fun fetchNotificationSettings(
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<NotificationSettingsDTO> =
        ResponseEntity.ok(userService.fetchNotificationSettings(user.id))

    @PutMapping("/notification-settings")
    fun updateNotificationSettings(
        @AuthenticationPrincipal user: UserDTO,
        @RequestBody settings: NotificationSettingsDTO,
    ): ResponseEntity<NotificationSettingsDTO> {
        val updated = userService.updateNotificationSettings(user.id, settings)
        log.info("Updated user notification settings userId={}", user.id)
        return ResponseEntity.ok(updated)
    }
}
