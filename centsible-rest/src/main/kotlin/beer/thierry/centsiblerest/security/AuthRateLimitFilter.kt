package beer.thierry.centsiblerest.security

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Duration

@Component
class AuthRateLimitFilter : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(AuthRateLimitFilter::class.java)

    private val buckets = Caffeine.newBuilder()
        .maximumSize(100_000)
        .expireAfterAccess(Duration.ofMinutes(15))
        .build<String, Bucket>()

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val uri = request.requestURI ?: return true
        if (uri in RATE_LIMITED_PATHS) return false
        if (uri.startsWith("/api/integrations/oauth/")) return false
        if (uri.endsWith("/sync") && uri.startsWith("/api/integrations/connections/")) return false
        if (uri.endsWith("/oauth/start") && uri.startsWith("/api/integrations/connections/")) return false
        if (uri.startsWith("/api/integrations/providers/") && uri.contains("/options/")) return false
        return true
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val bucket = buckets.get(request.remoteAddr ?: "unknown") { newBucket() }
        val probe = bucket.tryConsumeAndReturnRemaining(1)
        if (probe.isConsumed) {
            response.setHeader("X-RateLimit-Remaining", probe.remainingTokens.toString())
            filterChain.doFilter(request, response)
            return
        }
        val retryAfterSeconds = (probe.nanosToWaitForRefill / 1_000_000_000).coerceAtLeast(1)
        log.warn(
            "Rate limit exceeded path={} ip={} retryAfterSeconds={}",
            request.requestURI, request.remoteAddr, retryAfterSeconds,
        )
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.setHeader("Retry-After", retryAfterSeconds.toString())
        response.contentType = "application/json"
        response.writer.write("""{"message":"Too many requests, please try again later."}""")
    }

    private fun newBucket(): Bucket = Bucket.builder()
        .addLimit(Bandwidth.builder().capacity(10).refillIntervally(10, Duration.ofMinutes(1)).build())
        .build()

    private companion object {
        val RATE_LIMITED_PATHS = setOf(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/confirm",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/2fa/challenge",
            "/api/auth/2fa/confirm",
            "/api/auth/2fa/disable",
            "/api/auth/2fa/enroll",
            "/api/auth/change-password",
            "/api/auth/account/delete",
        )
    }
}
