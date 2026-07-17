package beer.thierry.centsiblerest.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.ConsumptionProbe
import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.time.Duration
import java.util.Locale

@Component
class AuthRateLimitFilter(private val objectMapper: ObjectMapper) : OncePerRequestFilter() {
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
        val ipProbe = consume("ip:${request.remoteAddr ?: "unknown"}")
        if (!ipProbe.isConsumed) return reject(request, response, "ip", ipProbe)

        val body = bufferedBody(request)
        val forwarded = body?.let { CachedBodyRequest(request, it) } ?: request
        val subject = subjectKey(request, body)

        var remaining = ipProbe.remainingTokens
        if (subject != null) {
            val subjectProbe = consume(subject)
            if (!subjectProbe.isConsumed) {
                return reject(request, response, subject.substringBefore(':'), subjectProbe)
            }
            remaining = minOf(remaining, subjectProbe.remainingTokens)
        }

        response.setHeader("X-RateLimit-Remaining", remaining.toString())
        filterChain.doFilter(forwarded, response)
    }

    private fun consume(key: String): ConsumptionProbe =
        buckets.get(key) { newBucket() }.tryConsumeAndReturnRemaining(1)

    private fun reject(
        request: HttpServletRequest,
        response: HttpServletResponse,
        scope: String,
        probe: ConsumptionProbe,
    ) {
        val retryAfterSeconds = (probe.nanosToWaitForRefill / 1_000_000_000).coerceAtLeast(1)
        log.warn(
            "Rate limit exceeded path={} ip={} scope={} retryAfterSeconds={}",
            request.requestURI, request.remoteAddr, scope, retryAfterSeconds,
        )
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.setHeader("Retry-After", retryAfterSeconds.toString())
        response.contentType = "application/json"
        response.writer.write("""{"message":"Too many requests, please try again later."}""")
    }

    /**
     * The account a request is aimed at, throttled alongside the caller's IP so that neither
     * identity is the only limit. /login and /forgot-password name it in the body; the TOTP
     * challenge doesn't carry a username at all, and its pre-auth cookie is what pins it to one
     * pending login.
     */
    private fun subjectKey(request: HttpServletRequest, body: ByteArray?): String? =
        if (request.requestURI == TOTP_CHALLENGE_PATH) {
            request.cookies?.firstOrNull { it.name == PRE_AUTH_COOKIE_NAME }
                ?.value?.takeIf { it.isNotBlank() }
                ?.let { "pre-auth:$it" }
        } else {
            body?.let(::username)?.let { "user:$it" }
        }

    private fun username(body: ByteArray): String? = try {
        objectMapper.readTree(body).path("username").asText("")
            .trim().lowercase(Locale.ROOT).ifBlank { null }
    } catch (ex: IOException) {
        log.debug("Could not read username from auth request body: {}", ex.message)
        null
    }

    /**
     * Buffers the body of the username-bearing endpoints so [CachedBodyRequest] can replay it.
     * Anything without a declared length (chunked) or larger than a credential payload is left
     * untouched and throttled on IP alone rather than read.
     */
    private fun bufferedBody(request: HttpServletRequest): ByteArray? {
        if (request.requestURI !in USERNAME_BODY_PATHS) return null
        if (request.contentLengthLong !in 1..MAX_SUBJECT_BODY_BYTES) return null
        if (request.contentType?.startsWith(MediaType.APPLICATION_JSON_VALUE, ignoreCase = true) != true) return null
        return try {
            request.inputStream.readAllBytes()
        } catch (ex: IOException) {
            log.debug("Could not buffer auth request body: {}", ex.message)
            null
        }
    }

    private fun newBucket(): Bucket = Bucket.builder()
        .addLimit(Bandwidth.builder().capacity(10).refillIntervally(10, Duration.ofMinutes(1)).build())
        .build()

    /**
     * Replays a body this filter has already consumed so the controller's @RequestBody still binds.
     * ContentCachingRequestWrapper caches on the way past and cannot re-serve a spent stream, which
     * is the read order a rate limiter needs.
     */
    private class CachedBodyRequest(
        request: HttpServletRequest,
        private val body: ByteArray,
    ) : HttpServletRequestWrapper(request) {

        override fun getInputStream(): ServletInputStream = object : ServletInputStream() {
            private val delegate = ByteArrayInputStream(body)
            override fun read(): Int = delegate.read()
            override fun available(): Int = delegate.available()
            override fun isFinished(): Boolean = delegate.available() == 0
            override fun isReady(): Boolean = true
            override fun setReadListener(listener: ReadListener) = throw UnsupportedOperationException()
        }

        override fun getReader(): BufferedReader =
            BufferedReader(InputStreamReader(inputStream, characterEncoding ?: Charsets.UTF_8.name()))
    }

    private companion object {
        const val TOTP_CHALLENGE_PATH = "/api/auth/2fa/challenge"
        const val MAX_SUBJECT_BODY_BYTES = 8L * 1024

        val USERNAME_BODY_PATHS = setOf(
            "/api/auth/login",
            "/api/auth/forgot-password",
        )

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
