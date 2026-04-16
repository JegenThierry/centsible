package beer.thierry.budgetplanner.core.service

import beer.thierry.budgetplanner.api.service.auth.IAuthService
import beer.thierry.budgetplanner.api.model.auth.AuthRegisterRequest
import beer.thierry.budgetplanner.api.model.auth.AuthRequest
import beer.thierry.budgetplanner.api.model.auth.AuthResponse
import beer.thierry.budgetplanner.api.model.user.User
import beer.thierry.budgetplanner.api.repository.IUserRepository
import beer.thierry.budgetplanner.api.service.email.IRegisterEmailService
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.SecretKey

@Service
class AuthService(
    private val userRepository: IUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val registerEmailService: IRegisterEmailService,
    @Value("\${skip.email.verification}") private val skipEmailVerification: Boolean,
    @Value("\${jwt.secret}") private val jwtSecret: String,
    @Value("\${jwt.expirationMs}") private val jwtExpirationMs: Long
) : IAuthService {
    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))
    }

    override fun authenticate(authRequest: AuthRequest): AuthResponse {
        val user = userRepository.findUserByUsername(authRequest.username)
            ?: throw IllegalArgumentException("Invalid username or password")

        if (!passwordEncoder.matches(authRequest.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid username or password" )
        }

        if (!user.registered) {
            throw IllegalArgumentException("Please confirm your email address")
        }

        return AuthResponse(generateJwt(user))
    }

    override fun register(authRequest: AuthRegisterRequest): AuthResponse {
        assertPasswordMatchesSecuritySettings(authRequest.password)
        val user = userRepository.findUserByEmailOrUsername(authRequest.email, authRequest.username)
        if (user != null) {
            throw IllegalArgumentException("An account with these credentials already exists")
        }

        val registeredUser = userRepository.createUser(authRequest)
            ?: throw IllegalArgumentException("User could not be created")

        if(skipEmailVerification) {
            return AuthResponse(generateJwt(registeredUser))
        }

        registerEmailService.sendRegistrationEmail(registeredUser, registeredUser.registrationToken.toString())
        return AuthResponse("")
    }

    override fun confirmRegistration(token: String, username: String): Boolean {
        val user = userRepository.findUserByTokenAndUsername(UUID.fromString(token), username) ?: return false
        return userRepository.confirmUser(user.id)
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
            .claim("profilePicture", user.profilePicture)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact()
    }

    /**
     * The password needs to match the following criteria:
     * - Needs to be at least 8 chars long
     * - Needs to contain at least 1 uppercase letter
     * - Needs to contain at least 1 lowercase letter
     * - Needs to contain at least 1 number
     * - Needs to contain at least 1 of the following special characters: @ $ ! % * ? &
     * @throws IllegalArgumentException when the password does not match the above criteria.
     */
    private fun assertPasswordMatchesSecuritySettings(password: String) {
        val passwordRegex = Regex("""^(?=.*[A-Z])(?=.*[a-z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$""")
        if(!password.matches(passwordRegex)) {
            throw IllegalArgumentException("Password does not meet security requirements")
        }
    }
}
