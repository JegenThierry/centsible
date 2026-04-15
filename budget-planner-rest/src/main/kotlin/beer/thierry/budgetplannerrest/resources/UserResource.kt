package beer.thierry.budgetplannerrest.resources

import beer.thierry.budgetplanner.api.model.user.ProfileUpdateDTO
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.service.users.IUserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.*

@RequestMapping("/api/users")
@RestController
class UserResource(private val userService: IUserService) {

    @GetMapping("")
    fun getAllUsers(): ResponseEntity<List<UserDTO>> {
        return ResponseEntity.ok(userService.fetchAllUsers())
    }

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
        @RequestBody profile: ProfileUpdateDTO
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
        val base64 = Base64.getEncoder().encodeToString(file.bytes)
        val dataUrl = "data:${file.contentType};base64,$base64"
        return ResponseEntity.ok(userService.updateProfilePicture(user.id, dataUrl))
    }
}
