package beer.thierry.centsible.core.services.authentication

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.auth.AuthRegisterRequest
import beer.thierry.centsible.api.model.auth.AuthRequest
import beer.thierry.centsible.api.model.auth.AuthResponse
import beer.thierry.centsible.api.model.auth.LoginResult
import beer.thierry.centsible.api.model.user.User
import beer.thierry.centsible.api.repository.IMfaRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.authentication.IAuthService
import beer.thierry.centsible.api.services.authentication.ITotpService
import beer.thierry.centsible.api.services.email.IPasswordResetEmailService
import beer.thierry.centsible.api.services.email.IRegisterEmailService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
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
    private val mfaRepository: IMfaRepository,
    private val totpService: ITotpService,
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

    private val dummyPasswordHash: String by lazy {
        passwordEncoder.encode(DUMMY_PASSWORD) ?: error("password encoder returned a null hash")
    }

    override fun authenticate(authRequest: AuthRequest): LoginResult {
        val user = userRepository.findUserByUsername(authRequest.username)
        if (user == null) {
            passwordEncoder.matches(authRequest.password, dummyPasswordHash)
            log.warn("Authentication failed: unknown username='{}'", authRequest.username)
            throw LocalizedException.Unauthorized("error.auth.invalidCredentials")
        }

        if (!passwordEncoder.matches(authRequest.password, user.passwordHash)) {
            log.warn("Authentication failed: bad password for userId={}", user.id)
            throw LocalizedException.Unauthorized("error.auth.invalidCredentials")
        }

        if (!user.registered) {
            log.warn("Authentication blocked: unconfirmed account userId={}", user.id)
            throw LocalizedException.Unauthorized("error.auth.emailNotConfirmed")
        }

        if (user.totpEnabled) {
            // Password is correct but a second factor is required: issue a short-lived, single-use
            // pre-auth token instead of the real JWT. The challenge step redeems it.
            val rawToken = generateRegistrationToken()
            val expiresAt = OffsetDateTime.now().plusMinutes(PRE_AUTH_TOKEN_TTL_MINUTES)
            // Drop any stale pending tokens for this user so only the freshest attempt is live.
            mfaRepository.deletePendingAuthForUser(user.id)
            mfaRepository.createPendingAuth(user.id, sha256(rawToken), expiresAt)
            log.info("Authentication step 1 ok; awaiting 2FA challenge userId={}", user.id)
            return LoginResult.TwoFactorRequired(rawToken)
        }

        log.info("Authentication successful: userId={}", user.id)
        return LoginResult.Authenticated(generateJwt(user))
    }

    override fun completeTwoFactorChallenge(pendingToken: String, code: String): AuthResponse {
        if (pendingToken.isBlank() || pendingToken.length > REGISTRATION_TOKEN_MAX_LENGTH) {
            throw LocalizedException.Unauthorized("error.auth.invalidCredentials")
        }
        val userId = mfaRepository.consumePendingAuth(sha256(pendingToken))
            ?: throw LocalizedException.Unauthorized("error.totp.challengeExpired")

        if (!totpService.verifyChallengeCode(userId, code)) {
            log.warn("2FA challenge failed: bad code userId={}", userId)
            throw LocalizedException.Unauthorized("error.totp.invalidCode")
        }

        val user = userRepository.findUserById(userId)
            ?: throw LocalizedException.Unauthorized("error.auth.invalidCredentials")
        log.info("2FA challenge passed; authentication successful userId={}", userId)
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
        log.info("Registered new user userId={} username='{}'", registeredUser.id, registeredUser.username)

        if (skipEmailVerification) {
            userRepository.confirmUser(registeredUser.id)
            log.info("Auto-confirmed user userId={} (email verification disabled)", registeredUser.id)
            return AuthResponse(generateJwt(registeredUser))
        }

        // Use the locale the user just chose during registration so the confirmation email
        // arrives in their language, not whatever the calling request's Accept-Language said.
        registerEmailService.sendRegistrationEmail(registeredUser, rawToken, resolveEmailLocale(registeredUser.locale))
        return AuthResponse("")
    }

    override fun confirmRegistration(token: String): Boolean {
        if (token.isBlank() || token.length > REGISTRATION_TOKEN_MAX_LENGTH) {
            log.warn("Registration confirmation rejected: token format invalid")
            return false
        }
        val user = userRepository.findUserByValidTokenHash(sha256(token))
        if (user == null) {
            log.warn("Registration confirmation rejected: no valid token match")
            return false
        }
        val confirmed = userRepository.confirmUser(user.id)
        if (confirmed) {
            log.info("Confirmed user registration userId={}", user.id)
        }
        return confirmed
    }

    // Runs off the request thread so the controller can write its 204 with branch-independent latency.
    // Otherwise a valid+confirmed username pays a full synchronous Resend round-trip (the token UPDATE
    // plus the outbound email) while every other input returns near-instantly — a timing oracle that
    // defeats the "always 204" enumeration defence. Fire-and-forget: failures are logged, never surfaced.
    @Async
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
        if (token.isBlank() || token.length > REGISTRATION_TOKEN_MAX_LENGTH) {
            log.warn("Password reset rejected: token format invalid")
            return false
        }
        assertPasswordMatchesSecuritySettings(newPassword)

        val user = userRepository.findUserByValidPasswordResetTokenHash(sha256(token))
        if (user == null) {
            log.warn("Password reset rejected: no valid token match")
            return false
        }
        val passwordHash = passwordEncoder.encode(newPassword)
            ?: throw LocalizedException.InternalError("error.auth.passwordHashFailed")
        val reset = userRepository.resetPassword(user.id, passwordHash)
        if (reset) {
            log.info("Password reset successful userId={}", user.id)
        } else {
            log.warn("Password reset failed during persistence userId={}", user.id)
        }
        return reset
    }

    override fun changePassword(userId: UUID, currentPassword: String, newPassword: String): String {
        val user = userRepository.findUserById(userId)
            ?: throw LocalizedException.Unauthorized("error.auth.invalidCredentials")

        if (!passwordEncoder.matches(currentPassword, user.passwordHash)) {
            log.warn("Password change rejected: bad current password userId={}", userId)
            throw LocalizedException.Unauthorized("error.auth.invalidCredentials")
        }

        assertPasswordMatchesSecuritySettings(newPassword)

        if (passwordEncoder.matches(newPassword, user.passwordHash)) {
            throw LocalizedException.BadRequest("error.auth.passwordReused")
        }

        val passwordHash = passwordEncoder.encode(newPassword)
            ?: throw LocalizedException.InternalError("error.auth.passwordHashFailed")
        if (!userRepository.updatePassword(userId, passwordHash)) {
            throw LocalizedException.InternalError("error.auth.passwordHashFailed")
        }
        // Revoke every other live session, then mint a fresh token so the caller stays signed in.
        userRepository.incrementTokenVersion(userId)
        log.info("Password changed successfully; other sessions revoked userId={}", userId)
        return issueFreshToken(userId)
    }

    override fun signOutOtherSessions(userId: UUID): String {
        if (!userRepository.incrementTokenVersion(userId)) {
            throw LocalizedException.Unauthorized("error.auth.invalidCredentials")
        }
        log.info("Signed out all other sessions userId={}", userId)
        return issueFreshToken(userId)
    }

    /** Mints a JWT reflecting the user's current persisted state (incl. token_version). */
    private fun issueFreshToken(userId: UUID): String {
        val refreshed = userRepository.findUserById(userId)
            ?: throw LocalizedException.Unauthorized("error.auth.invalidCredentials")
        return generateJwt(refreshed)
    }

    override fun deleteAccount(userId: UUID, password: String, totpCode: String?) {
        val user = userRepository.findUserById(userId)
            ?: throw LocalizedException.Unauthorized("error.auth.invalidCredentials")

        if (!passwordEncoder.matches(password, user.passwordHash)) {
            log.warn("Account deletion rejected: bad password userId={}", userId)
            throw LocalizedException.Unauthorized("error.auth.invalidCredentials")
        }

        if (user.totpEnabled) {
            // 2FA users must additionally prove a current factor for this irreversible action.
            val code = totpCode?.takeIf { it.isNotBlank() }
                ?: throw LocalizedException.Unauthorized("error.totp.invalidCode")
            if (!totpService.verifyChallengeCode(userId, code)) {
                log.warn("Account deletion rejected: bad TOTP code userId={}", userId)
                throw LocalizedException.Unauthorized("error.totp.invalidCode")
            }
        }

        if (!userRepository.deleteUser(userId)) {
            throw LocalizedException.InternalError("error.user.deleteFailed")
        }
        log.info("Account deleted userId={}", userId)
    }

    private fun generateRegistrationToken(): String {
        val bytes = ByteArray(REGISTRATION_TOKEN_BYTES).also { SecureRandom().nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sha256(input: String): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))

    companion object {
        private const val DUMMY_PASSWORD = "centsible-timing-equalizer"
        private const val REGISTRATION_TOKEN_BYTES = 32
        private const val REGISTRATION_TOKEN_MAX_LENGTH = 64
        private const val REGISTRATION_TOKEN_TTL_HOURS = 24L
        private const val PASSWORD_RESET_TOKEN_TTL_MINUTES = 15L
        private const val PRE_AUTH_TOKEN_TTL_MINUTES = 5L
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
            // Embeds the user's session generation; the JWT filter rejects tokens whose tv no longer
            // matches the stored value (sign-out-everywhere / password change / 2FA disable).
            .claim("tv", user.tokenVersion)
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
