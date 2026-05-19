package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.notification.NotificationSettingsDTO
import beer.thierry.centsible.api.model.user.LocaleUpdateDTO
import beer.thierry.centsible.api.model.user.ProfileUpdateDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.users.IUserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RequestMapping("/api/users")
@RestController
class UserResource(private val userService: IUserService) {

    @GetMapping("/myself")
    fun getUserByUsername(@AuthenticationPrincipal user: UserDTO): ResponseEntity<UserDTO> =
        ResponseEntity.ok(userService.fetchUserByUsername(user.username))

    @PutMapping("/profile")
    fun updateProfile(
        @AuthenticationPrincipal user: UserDTO,
        @Valid @RequestBody profile: ProfileUpdateDTO,
    ): ResponseEntity<UserDTO> =
        ResponseEntity.ok(userService.updateUserProfile(user.id, profile))

    @PostMapping("/profile/picture")
    fun updateProfilePicture(
        @AuthenticationPrincipal user: UserDTO,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<UserDTO> =
        ResponseEntity.ok(userService.updateProfilePicture(user.id, file.toValidatedImageDataUrl()))

    @PutMapping("/locale")
    fun updateLocale(
        @AuthenticationPrincipal user: UserDTO,
        @Valid @RequestBody body: LocaleUpdateDTO,
    ): ResponseEntity<UserDTO> =
        ResponseEntity.ok(userService.updateUserLocale(user.id, body.locale))

    @GetMapping("/notification-settings")
    fun fetchNotificationSettings(
        @AuthenticationPrincipal user: UserDTO,
    ): ResponseEntity<NotificationSettingsDTO> =
        ResponseEntity.ok(userService.fetchNotificationSettings(user.id))

    @PutMapping("/notification-settings")
    fun updateNotificationSettings(
        @AuthenticationPrincipal user: UserDTO,
        @RequestBody settings: NotificationSettingsDTO,
    ): ResponseEntity<NotificationSettingsDTO> =
        ResponseEntity.ok(userService.updateNotificationSettings(user.id, settings))
}
