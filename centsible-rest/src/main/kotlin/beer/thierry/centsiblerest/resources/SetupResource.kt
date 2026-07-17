package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.auth.AuthRegisterRequest
import beer.thierry.centsible.api.model.auth.AuthResponse
import beer.thierry.centsible.api.model.setup.SetupStatusResponse
import beer.thierry.centsible.api.services.authentication.IAuthService
import beer.thierry.centsiblerest.security.AuthCookieIssuer
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/setup")
@RestController
class SetupResource(
    private val authService: IAuthService,
    private val authCookieIssuer: AuthCookieIssuer,
) {
    private val log = LoggerFactory.getLogger(SetupResource::class.java)

    @GetMapping
    fun status(): ResponseEntity<SetupStatusResponse> =
        ResponseEntity.ok(SetupStatusResponse(authService.needsSetup()))

    @PostMapping
    fun create(
        @Valid @RequestBody form: AuthRegisterRequest,
        response: HttpServletResponse,
    ): ResponseEntity<AuthResponse> {
        val result = authService.setupFirstUser(form)
        authCookieIssuer.issue(response, result.token)
        log.info("First-run setup completed for username={}", form.username)
        return ResponseEntity.ok(result)
    }
}
