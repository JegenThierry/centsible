package beer.thierry.centsible.core.services.authentication

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.auth.AuthRegisterRequest
import beer.thierry.centsible.api.model.auth.AuthRequest
import beer.thierry.centsible.api.model.auth.AuthResponse
import beer.thierry.centsible.api.model.user.User
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.authentication.IAuthService
import beer.thierry.centsible.api.services.email.IPasswordResetEmailService
import beer.thierry.centsible.api.services.email.IRegisterEmailService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.OffsetDateTime
import java.util.*
import javax.crypto.SecretKey

private val SUPPORTED_EMAIL_LOCALES = setOf("en", "fr", "de")

private fun resolveEmailLocale(stored: String?): Locale {
    val code = stored?.takeIf { it in SUPPORTED_EMAIL_LOCALES } ?: "en"
    return Locale.forLanguageTag(code)
}

@Service
class AuthenticationService(
    private val userRepository: IUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val registerEmailService: IRegisterEmailService,
    private val passwordResetEmailService: IPasswordResetEmailService,
    @Value("\${skip.email.verification}") private val skipEmailVerification: Boolean,
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${jwt.expiration-ms}") private val jwtExpirationMs: Long,
    @Value("\${registration.enabled:false}") private val registrationEnabled: Boolean,
) : IAuthService {
    private val log = LoggerFactory.getLogger(AuthenticationService::class.java)

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))
    }

    override fun authenticate(authRequest: AuthRequest): AuthResponse {
        val user = userRepository.findUserByUsername(authRequest.username)
            ?: throw LocalizedException.Unauthorized("error.auth.invalidCredentials")

        if (!passwordEncoder.matches(authRequest.password, user.passwordHash)) {
            throw LocalizedException.Unauthorized("error.auth.invalidCredentials")
        }

        if (!user.registered) {
            throw LocalizedException.Unauthorized("error.auth.emailNotConfirmed")
        }

        return AuthResponse(generateJwt(user))
    }

    override fun register(authRequest: AuthRegisterRequest): AuthResponse {
        if (!registrationEnabled) throw LocalizedException.Forbidden("error.auth.registrationDisabled")
        assertPasswordMatchesSecuritySettings(authRequest.password)

        val existing = userRepository.findUserByEmailOrUsername(authRequest.email, authRequest.username)
        if (existing != null) {
            log.info("Registration rejected: account already exists for username='{}' or email='{}'", authRequest.username, authRequest.email)
            throw LocalizedException.Conflict("error.auth.accountExists")
        }

        val passwordHash = passwordEncoder.encode(authRequest.password)
            ?: throw LocalizedException.InternalError("error.auth.passwordHashFailed")

        val rawToken = generateRegistrationToken()
        val tokenHash = sha256(rawToken)
        val tokenExpiresAt = OffsetDateTime.now().plusHours(REGISTRATION_TOKEN_TTL_HOURS)

        val registeredUser = userRepository.createUser(authRequest, passwordHash, tokenHash, tokenExpiresAt)
            ?: throw LocalizedException.InternalError("error.auth.userCreateFailed")

        if (skipEmailVerification) {
            userRepository.confirmUser(registeredUser.id)
            return AuthResponse(generateJwt(registeredUser))
        }

        // Use the locale the user just chose during registration so the confirmation email
        // arrives in their language, not whatever the calling request's Accept-Language said.
        registerEmailService.sendRegistrationEmail(registeredUser, rawToken, resolveEmailLocale(registeredUser.locale))
        return AuthResponse("")
    }

    override fun confirmRegistration(token: String): Boolean {
        if (token.isBlank() || token.length > REGISTRATION_TOKEN_MAX_LENGTH) return false
        val user = userRepository.findUserByValidTokenHash(sha256(token)) ?: return false
        return userRepository.confirmUser(user.id)
    }

    override fun requestPasswordReset(username: String) {
        // Always silent: don't leak which usernames exist. Trim only — no other normalisation,
        // since findUserByUsername is case-sensitive on the username column.
        val trimmed = username.trim().ifBlank { return }
        val user = userRepository.findUserByUsername(trimmed) ?: return

        if (!user.registered) {
            // Only registered (email-confirmed) accounts can reset; otherwise the confirmation flow applies.
            log.info("Password reset requested for unconfirmed account username='{}'; skipping email", trimmed)
            return
        }

        val rawToken = generateRegistrationToken()
        val expiresAt = OffsetDateTime.now().plusMinutes(PASSWORD_RESET_TOKEN_TTL_MINUTES)

        if (!userRepository.setPasswordResetToken(user.id, sha256(rawToken), expiresAt)) {
            log.warn("Failed to persist password reset token for user id='{}'", user.id)
            return
        }

        passwordResetEmailService.sendPasswordResetEmail(user, rawToken, resolveEmailLocale(user.locale))
    }

    override fun resetPassword(token: String, newPassword: String): Boolean {
        if (token.isBlank() || token.length > REGISTRATION_TOKEN_MAX_LENGTH) return false
        assertPasswordMatchesSecuritySettings(newPassword)

        val user = userRepository.findUserByValidPasswordResetTokenHash(sha256(token)) ?: return false
        val passwordHash = passwordEncoder.encode(newPassword)
            ?: throw LocalizedException.InternalError("error.auth.passwordHashFailed")
        return userRepository.resetPassword(user.id, passwordHash)
    }

    private fun generateRegistrationToken(): String {
        val bytes = ByteArray(REGISTRATION_TOKEN_BYTES).also { SecureRandom().nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(input: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))

    companion object {
        private const val REGISTRATION_TOKEN_BYTES = 32
        // 32 bytes base64url-encoded (no padding) is exactly 43 chars; cap a bit higher for safety.
        private const val REGISTRATION_TOKEN_MAX_LENGTH = 64
        private const val REGISTRATION_TOKEN_TTL_HOURS = 24L
        private const val PASSWORD_RESET_TOKEN_TTL_MINUTES = 15L
        private val PASSWORD_REQUIREMENTS = Regex("""^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$""")
    }

    private fun generateJwt(user: User): String {
        val now = Date()
        val expiry = Date(now.time + jwtExpirationMs)

        return Jwts.builder()
            .subject(user.id.toString())
            .claim("username", user.username)
            .claim("email", user.email)
            .claim("firstName", user.firstName)
            .claim("lastName", user.lastName)
            .claim("name", "${user.firstName} ${user.lastName}")
            .claim("locale", user.locale)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact()
    }

    private fun assertPasswordMatchesSecuritySettings(password: String) {
        if (!password.matches(PASSWORD_REQUIREMENTS)) {
            throw LocalizedException.BadRequest("error.auth.passwordRequirements")
        }
    }
}
