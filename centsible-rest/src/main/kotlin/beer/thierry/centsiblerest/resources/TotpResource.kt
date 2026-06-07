package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.auth.RecoveryCodesDTO
import beer.thierry.centsible.api.model.auth.TotpCodeRequest
import beer.thierry.centsible.api.model.auth.TotpEnrollmentDTO
import beer.thierry.centsible.api.model.auth.TotpStatusDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.authentication.ITotpService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * TOTP two-factor enrollment and management for the signed-in user. All endpoints require
 * authentication (the login-challenge step lives on /api/auth/2fa/challenge, which runs pre-auth).
 */
@RequestMapping("/api/auth/2fa")
@RestController
class TotpResource(
    private val totpService: ITotpService,
) {
    private val log = LoggerFactory.getLogger(TotpResource::class.java)

    @GetMapping("/status")
    fun status(@AuthenticationPrincipal user: UserDTO): ResponseEntity<TotpStatusDTO> =
        ResponseEntity.ok(totpService.status(user.id))

    @PostMapping("/enroll")
    fun enroll(@AuthenticationPrincipal user: UserDTO): ResponseEntity<TotpEnrollmentDTO> {
        log.info("TOTP enrollment requested userId={}", user.id)
        return ResponseEntity.ok(totpService.beginEnrollment(user.id, user.email))
    }

    @PostMapping("/confirm")
    fun confirm(
        @AuthenticationPrincipal user: UserDTO,
        @Valid @RequestBody request: TotpCodeRequest,
    ): ResponseEntity<RecoveryCodesDTO> =
        ResponseEntity.ok(totpService.confirmEnrollment(user.id, request.code))

    @PostMapping("/enroll/cancel")
    fun cancelEnroll(@AuthenticationPrincipal user: UserDTO): ResponseEntity<Void> {
        totpService.cancelEnrollment(user.id)
        log.info("TOTP enrollment cancelled userId={}", user.id)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/disable")
    fun disable(
        @AuthenticationPrincipal user: UserDTO,
        @Valid @RequestBody request: TotpCodeRequest,
    ): ResponseEntity<Void> {
        totpService.disable(user.id, request.code)
        return ResponseEntity.noContent().build()
    }
}
