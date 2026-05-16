package beer.thierry.centsibleexport

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["beer.thierry.centsible", "beer.thierry.centsibleexport"])
@EnableScheduling
class CentsibleExportApplication

fun main(args: Array<String>) {
    runApplication<CentsibleExportApplication>(*args)
}
