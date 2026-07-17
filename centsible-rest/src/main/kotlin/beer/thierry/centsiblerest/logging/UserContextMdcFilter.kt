package beer.thierry.centsiblerest.logging

import beer.thierry.centsible.api.model.user.UserDTO
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

const val MDC_USER_ID = "userId"

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class UserContextMdcFilter : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        val userId = (principal as? UserDTO)?.id?.toString()
        try {
            if (userId != null) MDC.put(MDC_USER_ID, userId)
            filterChain.doFilter(request, response)
        } finally {
            if (userId != null) MDC.remove(MDC_USER_ID)
        }
    }
}
