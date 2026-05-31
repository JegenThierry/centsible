package beer.thierry.centsiblerest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["beer.thierry.centsible", "beer.thierry.centsiblerest"])
@EnableScheduling
// Enables @Async (Spring Boot's auto-configured task executor backs it). Currently used to dispatch
// the password-reset email off the request thread so /auth/forgot-password returns with uniform latency.
@EnableAsync
class CentsibleRestApplication

fun main(args: Array<String>) {
    runApplication<CentsibleRestApplication>(*args)
}
