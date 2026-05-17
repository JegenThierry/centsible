package beer.thierry.centsiblerest.security

import beer.thierry.centsible.api.model.user.UserDTO
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
import java.util.*

@Component
class JwtAuthenticationFilter(
    @Value($$"${jwt.secret}") private val secret: String,
) : OncePerRequestFilter() {
    private val key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        extractToken(request)
            ?.let { token -> parseUserDTO(token) }
            ?.let { user -> buildAuthentication(user, request) }
            ?.also { auth -> SecurityContextHolder.getContext().authentication = auth }

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
        request: HttpServletRequest
    ): UsernamePasswordAuthenticationToken {
        return UsernamePasswordAuthenticationToken(
            user,
            null,
            user.authorities
        ).also {
            it.details = WebAuthenticationDetailsSource().buildDetails(request)
        }
    }
}
