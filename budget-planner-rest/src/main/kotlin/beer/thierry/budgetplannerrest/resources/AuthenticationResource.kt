package beer.thierry.budgetplannerrest.resources

import beer.thierry.budgetplanner.api.model.auth.AuthRegisterRequest
import beer.thierry.budgetplanner.api.model.auth.AuthRequest
import beer.thierry.budgetplanner.api.model.auth.AuthResponse
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.services.authentication.IAuthService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/auth")
@RestController
class AuthenticationResource(private val authService: IAuthService) {
    @PostMapping("/login")
    fun login(@RequestBody request: AuthRequest): ResponseEntity<AuthResponse> {
        val response = authService.authenticate(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/register")
    fun register(@RequestBody form: AuthRegisterRequest): ResponseEntity<AuthResponse> {
        val response = authService.register(form)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/confirm")
    fun confirm(@RequestParam token: String, @RequestParam username: String): ResponseEntity<String> {
        val confirmed = authService.confirmRegistration(token, username)
        return if (confirmed) {
            ResponseEntity.ok("Account confirmed successfully")
        } else {
            ResponseEntity.badRequest().body("Invalid confirmation token")
        }
    }

    @GetMapping("/verify")
    fun verify(@AuthenticationPrincipal authenticatedUser: UserDTO?): ResponseEntity<String> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).body("Not authenticated")
        }
        return ResponseEntity.ok("ok")
    }
}
