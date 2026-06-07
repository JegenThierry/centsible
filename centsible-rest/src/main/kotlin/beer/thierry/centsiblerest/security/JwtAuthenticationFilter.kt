package beer.thierry.centsiblerest.security

import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.users.IUserService
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Base64
import java.util.UUID

@Component
class JwtAuthenticationFilter(
    @Value($$"${jwt.secret}") private val secret: String,
    private val userService: IUserService,
) : OncePerRequestFilter() {
    private val key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        extractToken(request)
            ?.let(::parseUserDTO)
            ?.let { user -> buildAuthentication(user, request) }
            ?.also { SecurityContextHolder.getContext().authentication = it }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
        // Cookie for browsers; Authorization header for curl/Bruno.
        val cookieToken = request.cookies?.firstOrNull { it.name == AUTH_COOKIE_NAME }?.value
        if (!cookieToken.isNullOrBlank()) return cookieToken

        return request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.substringAfter("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    private fun parseUserDTO(token: String): UserDTO? = try {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        val userId = claims.subject ?: throw JwtException("Missing subject claim")
        val username = claims["username"] as? String ?: throw JwtException("Missing username claim")
        val email = claims["email"] as? String ?: throw JwtException("Missing email claim")
        val firstName = claims["firstName"] as? String ?: ""
        val lastName = claims["lastName"] as? String ?: ""
        val name = claims["name"] as? String ?: ""
        // Pre-locale tokens won't carry the claim; default to English so legacy sessions still work.
        val locale = claims["locale"] as? String ?: "en"

        // Token revocation: the token's session generation must match the stored one. Pre-versioning
        // tokens carry no tv claim and read as 0; a user whose version was never bumped is also 0, so
        // legacy sessions keep working until the first sign-out-everywhere / password change / 2FA
        // disable bumps the stored value — at which point every older token (including legacy) fails.
        // A missing user (deleted account) yields null and is likewise rejected.
        val tokenVersion = (claims["tv"] as? Number)?.toInt() ?: 0
        val currentVersion = userService.currentTokenVersion(UUID.fromString(userId))
        if (currentVersion == null || currentVersion != tokenVersion) {
            logger.warn("JWT rejected: token version mismatch (token=$tokenVersion current=$currentVersion)")
            return null
        }

        // Profile picture is intentionally not in the JWT — base64 images would
        // bloat every request and overflow Tomcat's response header buffer at login.
        // Anything that needs the avatar fetches /api/users/myself.
        UserDTO(
            id = UUID.fromString(userId),
            username = username,
            email = email,
            firstName = firstName,
            lastName = lastName,
            name = name,
            profilePicture = null,
            locale = locale,
        )
    } catch (ex: JwtException) {
        logger.warn("JWT validation failed: ${ex.message}")
        null
    } catch (ex: Exception) {
        logger.error("Unexpected error during JWT parsing", ex)
        null
    }

    private fun buildAuthentication(
        user: UserDTO,
        request: HttpServletRequest,
    ): UsernamePasswordAuthenticationToken =
        UsernamePasswordAuthenticationToken(user, null, user.authorities).also {
            it.details = WebAuthenticationDetailsSource().buildDetails(request)
        }
}
