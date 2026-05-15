package beer.thierry.budgetplannerrest.resources

import beer.thierry.budgetplanner.api.model.auth.AuthRegisterRequest
import beer.thierry.budgetplanner.api.model.auth.AuthRequest
import beer.thierry.budgetplanner.api.model.auth.AuthResponse
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.services.authentication.IAuthService
import beer.thierry.budgetplannerrest.security.AuthCookieIssuer
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/auth")
@RestController
class AuthenticationResource(
    private val authService: IAuthService,
    private val authCookieIssuer: AuthCookieIssuer,
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: AuthRequest, response: HttpServletResponse): ResponseEntity<AuthResponse> {
        val authResult = authService.authenticate(request)
        authCookieIssuer.issue(response, authResult.token)
        // Token also in body so curl/Bruno can use Authorization header.
        return ResponseEntity.ok(AuthResponse(authResult.token))
    }

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody form: AuthRegisterRequest,
        response: HttpServletResponse,
    ): ResponseEntity<AuthResponse> {
        val authResult = authService.register(form)
        // Token is empty unless skip-email-verification is on; otherwise login follows confirm.
        if (authResult.token.isNotBlank()) {
            authCookieIssuer.issue(response, authResult.token)
        }
        return ResponseEntity.ok(authResult)
    }

    @GetMapping("/confirm")
    fun confirm(@RequestParam token: String): ResponseEntity<String> {
        val confirmed = authService.confirmRegistration(token)
        return if (confirmed) {
            ResponseEntity.ok("Account confirmed successfully")
        } else {
            ResponseEntity.badRequest().body("Invalid confirmation token")
        }
    }

    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {
        authCookieIssuer.clear(response)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/verify")
    fun verify(@AuthenticationPrincipal authenticatedUser: UserDTO?): ResponseEntity<String> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).body("Not authenticated")
        }
        return ResponseEntity.ok("ok")
    }
}
