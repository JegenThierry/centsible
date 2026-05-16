package beer.thierry.centsiblerest.resources

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
    fun getUserByUsername(@AuthenticationPrincipal user: UserDTO?): ResponseEntity<UserDTO> {
        if (user == null) {
            return ResponseEntity.status(401).build()
        }
        return ResponseEntity.ok(userService.fetchUserByUsername(user.username))
    }

    @PutMapping("/profile")
    fun updateProfile(
        @AuthenticationPrincipal user: UserDTO?,
        @Valid @RequestBody profile: ProfileUpdateDTO
    ): ResponseEntity<UserDTO> {
        if (user == null) {
            return ResponseEntity.status(401).build()
        }
        return ResponseEntity.ok(userService.updateUserProfile(user.id, profile))
    }

    @PostMapping("/profile/picture")
    fun updateProfilePicture(
        @AuthenticationPrincipal user: UserDTO?,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<UserDTO> {
        if (user == null) {
            return ResponseEntity.status(401).build()
        }
        val dataUrl = file.toValidatedImageDataUrl()
        return ResponseEntity.ok(userService.updateProfilePicture(user.id, dataUrl))
    }

    @PutMapping("/locale")
    fun updateLocale(
        @AuthenticationPrincipal user: UserDTO?,
        @Valid @RequestBody body: LocaleUpdateDTO,
    ): ResponseEntity<UserDTO> {
        if (user == null) {
            return ResponseEntity.status(401).build()
        }
        return ResponseEntity.ok(userService.updateUserLocale(user.id, body.locale))
    }
}
