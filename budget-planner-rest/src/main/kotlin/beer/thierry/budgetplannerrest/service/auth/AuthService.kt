package beer.thierry.budgetplannerrest.service.auth

import beer.thierry.budgetplannerrest.model.auth.AuthRegisterRequest
import beer.thierry.budgetplannerrest.model.auth.AuthRequest
import beer.thierry.budgetplannerrest.model.auth.AuthResponse
import beer.thierry.budgetplannerrest.repository.users.IUserRepository
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.Date
import java.util.UUID

@Service
class AuthService(
    private val userRepository: IUserRepository,
    @Value($$"${jwt.secret}") private val jwtSecret: String,
    @Value($$"${jwt.expirationMs}") private val jwtExpirationMs: Long
) : IAuthService {
    private val passwordEncoder = BCryptPasswordEncoder()

    override fun authenticate(authRequest: AuthRequest): AuthResponse {
        val user = userRepository.findUserByUsername(authRequest.username)
            ?: throw IllegalArgumentException("Invalid username or password")

        if (!passwordEncoder.matches(authRequest.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid username or password" )
        }

        val token = generateJwt(user.id, user.username)
        return AuthResponse(token)
    }

    override fun register(authRequest: AuthRegisterRequest): AuthResponse {
        assertPasswordMatchesSecuritySettings(authRequest.password)
        val user = userRepository.findUserByEmailOrUsername(authRequest.email, authRequest.username)
        if (user != null) {
            throw IllegalArgumentException("User with username ${authRequest.username} or ${authRequest.email} already exists")
        }

        val registeredUser = userRepository.createUser(authRequest)
            ?: throw IllegalArgumentException("User with username: ${authRequest.username} could not be created")

        val token = generateJwt(registeredUser.id, registeredUser.username)
        return AuthResponse(token)
    }

    private fun generateJwt(id: UUID, username: String): String {
        val now = Date()
        val expiryDate = Date(now.time + jwtExpirationMs)

        val key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret))

        return Jwts.builder()
            .setSubject(id.toString())
            .claim("username", username)
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .signWith(key, SignatureAlgorithm.HS256)
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
            throw IllegalArgumentException("Password does not enforce security standards")
        }
    }
}