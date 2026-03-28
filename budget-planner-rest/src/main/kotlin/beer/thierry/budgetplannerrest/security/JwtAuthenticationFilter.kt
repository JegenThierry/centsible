package beer.thierry.budgetplannerrest.security

import beer.thierry.budgetplannerrest.model.user.UserDTO
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Base64
import java.util.UUID

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

    private fun extractToken(request: HttpServletRequest): String? =
        request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.substringAfter("Bearer ")
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun parseUserDTO(token: String): UserDTO? = try {
        val claims = Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload

        val userId = claims.subject ?: throw JwtException("Missing subject claim")
        val username = claims["username"] as? String ?: throw JwtException("Missing username claim")
        val email = claims["email"] as? String ?: throw JwtException("Missing email claim")
        val name = claims["name"] as? String ?: throw JwtException("Missing name claim")

        UserDTO(
            id = UUID.fromString(userId),
            username = username,
            email = email,
            name = name,
            image = null,
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