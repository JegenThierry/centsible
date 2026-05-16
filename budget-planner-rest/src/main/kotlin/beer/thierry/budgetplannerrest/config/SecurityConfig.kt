package beer.thierry.budgetplannerrest.config

import beer.thierry.budgetplannerrest.security.AuthRateLimitFilter
import beer.thierry.budgetplannerrest.security.JwtAuthenticationFilter
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter


@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthenticationFilter,
    private val authRateLimitFilter: AuthRateLimitFilter,
    private val corsConfig: CorsConfig
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            cors {
                configurationSource = corsConfig.corsConfigurationSource()
            }

            // SameSite=Strict cookie + CORS allowlist defend against CSRF.
            // Re-enable if SameSite is ever loosened or form-encoded writes are accepted.
            csrf { disable() }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }

            headers {
                contentTypeOptions { }
                httpStrictTransportSecurity {
                    includeSubDomains = true
                    maxAgeInSeconds = 31_536_000
                }
                referrerPolicy {
                    policy = ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                }
                contentSecurityPolicy {
                    policyDirectives = "default-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'"
                }
            }

            authorizeHttpRequests {
                authorize("/api/auth/register", permitAll)
                authorize("/api/auth/login", permitAll)
                authorize("/api/auth/confirm", permitAll)
                authorize("/api/auth/logout", permitAll)
                authorize("/api/auth/forgot-password", permitAll)
                authorize("/api/auth/reset-password", permitAll)
                authorize("/api/system", permitAll)
                authorize("/api/integrations/oauth/callback/**", permitAll)
                authorize(EndpointRequest.to("health"), permitAll)
                authorize(anyRequest, authenticated)
            }

            exceptionHandling {
                authenticationEntryPoint = HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)
            }

            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthFilter)
            addFilterBefore<JwtAuthenticationFilter>(authRateLimitFilter)
        }

        return http.build()
    }

    @Bean
    fun authenticationManager(
        authConfig: AuthenticationConfiguration
    ): AuthenticationManager = authConfig.authenticationManager

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)
}
