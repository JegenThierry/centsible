package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.auth.RecoveryCodesDTO
import beer.thierry.centsible.api.model.auth.TotpCodeRequest
import beer.thierry.centsible.api.model.auth.TotpEnrollmentDTO
import beer.thierry.centsible.api.model.auth.TotpStatusDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.authentication.IAuthService
import beer.thierry.centsible.api.services.authentication.ITotpService
import beer.thierry.centsiblerest.security.AuthCookieIssuer
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/auth/2fa")
@RestController
class TotpResource(
    private val totpService: ITotpService,
    private val authService: IAuthService,
    private val authCookieIssuer: AuthCookieIssuer,
) {
    private val log = LoggerFactory.getLogger(TotpResource::class.java)

    @GetMapping("/status")
    fun status(@AuthenticationPrincipal user: UserDTO): TotpStatusDTO =
        totpService.status(user.id)

    @PostMapping("/enroll")
    fun enroll(@AuthenticationPrincipal user: UserDTO): TotpEnrollmentDTO {
        log.info("TOTP enrollment requested userId={}", user.id)
        return totpService.beginEnrollment(user.id, user.email)
    }

    @PostMapping("/confirm")
    fun confirm(
        @AuthenticationPrincipal user: UserDTO,
        @Valid @RequestBody request: TotpCodeRequest,
    ): RecoveryCodesDTO =
        totpService.confirmEnrollment(user.id, request.code)

    @PostMapping("/enroll/cancel")
    fun cancelEnroll(@AuthenticationPrincipal user: UserDTO): ResponseEntity<Void> {
        totpService.cancelEnrollment(user.id)
        log.info("TOTP enrollment cancelled userId={}", user.id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/recovery-codes/regenerate")
    fun regenerateRecoveryCodes(
        @AuthenticationPrincipal user: UserDTO,
        @Valid @RequestBody request: TotpCodeRequest,
    ): RecoveryCodesDTO {
        log.info("Recovery-code regeneration requested userId={}", user.id)
        return totpService.regenerateRecoveryCodes(user.id, request.code)
    }

    @PostMapping("/disable")
    fun disable(
        @AuthenticationPrincipal user: UserDTO,
        @Valid @RequestBody request: TotpCodeRequest,
        response: HttpServletResponse,
    ): ResponseEntity<Void> {
        totpService.disable(user.id, request.code)
        val token = authService.signOutOtherSessions(user.id)
        authCookieIssuer.issue(response, token)
        log.info("2FA disabled and other sessions revoked userId={}", user.id)
        return ResponseEntity.noContent().build()
    }
}
