package beer.thierry.budgetplannerrest.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.Base64

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
        print("Request $request")
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.substringAfter("Bearer ").trim()
            try {
                val claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .payload

                val userId = claims.subject
                val username = claims["username"] as String

                // TODO: Load roles from db
                val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))

                val auth = UsernamePasswordAuthenticationToken(username, null, authorities)
                SecurityContextHolder.getContext().authentication = auth
            } catch (ex: Exception) {
                // TODO: Handle Exception
            }
        }
        filterChain.doFilter(request, response)
    }
}