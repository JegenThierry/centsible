package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.auth.AuthRegisterRequest
import beer.thierry.centsible.api.model.auth.AuthRequest
import beer.thierry.centsible.api.model.auth.AuthResponse
import beer.thierry.centsible.api.model.auth.LoginResponse
import beer.thierry.centsible.api.model.auth.LoginResult
import beer.thierry.centsible.api.model.auth.PasswordChangeRequest
import beer.thierry.centsible.api.model.auth.PasswordResetConfirmRequest
import beer.thierry.centsible.api.model.auth.PasswordResetRequest
import beer.thierry.centsible.api.model.auth.TotpChallengeRequest
import beer.thierry.centsible.api.model.user.AccountDeletionRequest
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.authentication.IAuthService
import beer.thierry.centsible.api.services.users.IUserService
import beer.thierry.centsiblerest.security.AuthCookieIssuer
import beer.thierry.centsiblerest.security.PRE_AUTH_COOKIE_NAME
import beer.thierry.centsiblerest.security.PreAuthCookieIssuer
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
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
    private val preAuthCookieIssuer: PreAuthCookieIssuer,
) {
    private val log = LoggerFactory.getLogger(AuthenticationResource::class.java)

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: AuthRequest, response: HttpServletResponse): ResponseEntity<LoginResponse> {
        val result = try {
            authService.authenticate(request)
        } catch (ex: RuntimeException) {
            log.warn("Login failed for username={}", request.username)
            throw ex
        }
        return when (result) {
            is LoginResult.Authenticated -> {
                authCookieIssuer.issue(response, result.token)
                log.info("Login succeeded for username={}", request.username)
                ResponseEntity.ok(LoginResponse(twoFactorRequired = false, token = result.token))
            }
            is LoginResult.TwoFactorRequired -> {
                preAuthCookieIssuer.issue(response, result.pendingToken)
                log.info("Login step 1 ok for username={}; 2FA challenge required", request.username)
                ResponseEntity.status(HttpStatus.ACCEPTED).body(LoginResponse(twoFactorRequired = true))
            }
        }
    }

    @PostMapping("/2fa/challenge")
    fun twoFactorChallenge(
        @Valid @RequestBody request: TotpChallengeRequest,
        @CookieValue(name = PRE_AUTH_COOKIE_NAME, required = false) pendingToken: String?,
        response: HttpServletResponse,
    ): ResponseEntity<AuthResponse> {
        if (pendingToken.isNullOrBlank()) {
            log.warn("2FA challenge rejected: missing pre-auth cookie")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
        val authResult = try {
            authService.completeTwoFactorChallenge(pendingToken, request.code)
        } catch (ex: RuntimeException) {
            preAuthCookieIssuer.clear(response)
            throw ex
        }
        preAuthCookieIssuer.clear(response)
        authCookieIssuer.issue(response, authResult.token)
        log.info("2FA challenge succeeded; session issued")
        return ResponseEntity.ok(authResult)
    }

    @PostMapping("/register")
    fun register(
        @Valid @RequestBody form: AuthRegisterRequest,
        response: HttpServletResponse,
    ): ResponseEntity<AuthResponse> {
        val authResult = try {
            authService.register(form)
        } catch (ex: RuntimeException) {
            log.warn("Registration failed for username={}", form.username)
            throw ex
        }
        if (authResult.token.isNotBlank()) authCookieIssuer.issue(response, authResult.token)
        log.info(
            "Registration accepted for username={} pendingConfirmation={}",
            form.username,
            authResult.token.isBlank(),
        )
        return ResponseEntity.ok(authResult)
    }

    @GetMapping("/confirm")
    fun confirm(@RequestParam token: String): ResponseEntity<String> {
        val confirmed = authService.confirmRegistration(token)
        if (!confirmed) {
            log.warn("Registration confirmation rejected (invalid or expired token)")
            throw LocalizedException.BadRequest("error.auth.confirmInvalid")
        }
        log.info("Registration confirmation succeeded")
        return ResponseEntity.ok("ok")
    }

    @PostMapping("/logout")
    fun logout(response: HttpServletResponse): ResponseEntity<Void> {
        authCookieIssuer.clear(response)
        log.info("Logout: cleared auth cookie")
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody request: PasswordResetRequest): ResponseEntity<Void> {
        authService.requestPasswordReset(request.username)
        log.info("Password reset requested for username={}", request.username)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: PasswordResetConfirmRequest): ResponseEntity<Void> {
        val ok = authService.resetPassword(request.token, request.password)
        if (ok) {
            log.info("Password reset succeeded")
        } else {
            log.warn("Password reset rejected (invalid or expired token)")
        }
        return if (ok) ResponseEntity.noContent().build()
        else ResponseEntity.badRequest().build()
    }

    @PostMapping("/change-password")
    fun changePassword(
        @AuthenticationPrincipal user: UserDTO,
        @Valid @RequestBody request: PasswordChangeRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        val token = authService.changePassword(user.id, request.currentPassword, request.newPassword)
        authCookieIssuer.issue(response, token)
        log.info("Password changed and other sessions revoked userId={}", user.id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/sign-out-everywhere")
    fun signOutEverywhere(
        @AuthenticationPrincipal user: UserDTO,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        val token = authService.signOutOtherSessions(user.id)
        authCookieIssuer.issue(response, token)
        log.info("Signed out all other sessions userId={}", user.id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/account/delete")
    fun deleteAccount(
        @AuthenticationPrincipal user: UserDTO,
        @Valid @RequestBody request: AccountDeletionRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        authService.deleteAccount(user.id, request.password, request.totpCode)
        authCookieIssuer.clear(response)
        log.info("Account deleted and session cleared userId={}", user.id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/verify")
    fun verify(
        @AuthenticationPrincipal authenticatedUser: UserDTO,
        response: HttpServletResponse,
    ): ResponseEntity<String> {
        if (userService.userExists(authenticatedUser.id)) return ResponseEntity.ok("ok")
        authCookieIssuer.clear(response)
        log.warn("Verify failed: account no longer exists userId={}", authenticatedUser.id)
        throw LocalizedException.Unauthorized("error.auth.accountGone")
    }
}
