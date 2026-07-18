package beer.thierry.centsiblerest.logging

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

const val REQUEST_ID_HEADER = "X-Request-Id"
const val MDC_REQUEST_ID = "requestId"
const val MDC_METHOD = "method"
const val MDC_PATH = "path"

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestCorrelationFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val requestId = request.getHeader(REQUEST_ID_HEADER)?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString()
        response.setHeader(REQUEST_ID_HEADER, requestId)
        try {
            MDC.put(MDC_REQUEST_ID, requestId)
            MDC.put(MDC_METHOD, request.method)
            MDC.put(MDC_PATH, request.requestURI)
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(MDC_REQUEST_ID)
            MDC.remove(MDC_METHOD)
            MDC.remove(MDC_PATH)
        }
    }
}
