package beer.thierry.centsiblerest.security

import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.admin.IAdminAccessService
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
    private val adminAccessService: IAdminAccessService,
) : OncePerRequestFilter() {
    private val key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        extractToken(request)
            ?.let(::parseUserDTO)
            ?.also { user ->
                SecurityContextHolder.getContext().authentication = buildAuthentication(user, request)
                userService.recordUserActivity(user.id)
            }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? {
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
        val locale = claims["locale"] as? String ?: "en"

        val tokenVersion = (claims["tv"] as? Number)?.toInt() ?: 0
        val currentVersion = userService.currentTokenVersion(UUID.fromString(userId))
        if (currentVersion == null || currentVersion != tokenVersion) {
            logger.warn("JWT rejected: token version mismatch (token=$tokenVersion current=$currentVersion)")
            return null
        }

        UserDTO(
            id = UUID.fromString(userId),
            username = username,
            email = email,
            firstName = firstName,
            lastName = lastName,
            name = name,
            profilePicture = null,
            locale = locale,
            admin = adminAccessService.isAdmin(username),
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
