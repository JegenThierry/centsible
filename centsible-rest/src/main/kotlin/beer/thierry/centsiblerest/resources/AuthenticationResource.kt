package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.auth.AuthRegisterRequest
import beer.thierry.centsible.api.model.auth.AuthRequest
import beer.thierry.centsible.api.model.auth.AuthResponse
import beer.thierry.centsible.api.model.auth.PasswordResetConfirmRequest
import beer.thierry.centsible.api.model.auth.PasswordResetRequest
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.authentication.IAuthService
import beer.thierry.centsible.api.services.users.IUserService
import beer.thierry.centsiblerest.security.AuthCookieIssuer
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/auth")
@RestController
class AuthenticationResource(
    private val authService: IAuthService,
    private val userService: IUserService,
    private val authCookieIssuer: AuthCookieIssuer,
) {
    @PostMapping("/login")
    fun login(@Valid @RequestBody request: AuthRequest, response: HttpServletResponse): ResponseEntity<AuthResponse> {
        val authResult = authService.authenticate(request)
        authCookieIssuer.issue(response, authResult.token)
        return ResponseEntity.ok(authResult)
    }

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody form: AuthRegisterRequest,
        response: HttpServletResponse,
    ): ResponseEntity<AuthResponse> {
        val authResult = authService.register(form)
        // Token is blank when registration is pending email confirmation; skip cookie in that case.
        if (authResult.token.isNotBlank()) authCookieIssuer.issue(response, authResult.token)
        return ResponseEntity.ok(authResult)
    }

    @GetMapping("/confirm")
    fun confirm(@RequestParam token: String): ResponseEntity<String> =
        if (authService.confirmRegistration(token)) ResponseEntity.ok("Account confirmed successfully")
        else ResponseEntity.badRequest().body("Invalid confirmation token")

    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {
        authCookieIssuer.clear(response)
        return ResponseEntity.noContent().build()
    }

    // Always 204 regardless of whether the username exists — prevents account enumeration.
    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody request: PasswordResetRequest): ResponseEntity<Void> {
        authService.requestPasswordReset(request.username)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: PasswordResetConfirmRequest): ResponseEntity<Void> =
        if (authService.resetPassword(request.token, request.password)) ResponseEntity.noContent().build()
        else ResponseEntity.badRequest().build()

    @GetMapping("/verify")
    fun verify(
        @AuthenticationPrincipal authenticatedUser: UserDTO,
        response: HttpServletResponse,
    ): ResponseEntity<String> {
        // The JWT filter rebuilds the principal from claims alone, so a valid token can
        // outlive the user. Reject — and clear the cookie — when that happens.
        if (userService.userExists(authenticatedUser.id)) return ResponseEntity.ok("ok")
        authCookieIssuer.clear(response)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Account no longer exists")
    }
}
