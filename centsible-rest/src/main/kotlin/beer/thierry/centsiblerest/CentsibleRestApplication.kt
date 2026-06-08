package beer.thierry.centsiblerest

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["beer.thierry.centsible", "beer.thierry.centsiblerest"])
@EnableScheduling
@EnableAsync
class CentsibleRestApplication

fun main(args: Array<String>) {
    runApplication<CentsibleRestApplication>(*args)
}
