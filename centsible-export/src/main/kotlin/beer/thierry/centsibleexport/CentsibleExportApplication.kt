package beer.thierry.centsibleexport

import beer.thierry.centsibleexport.worker.WorkerProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.runApplication
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component

@SpringBootApplication(scanBasePackages = ["beer.thierry.centsible", "beer.thierry.centsibleexport"])
@EnableScheduling
class CentsibleExportApplication

fun main(args: Array<String>) {
    runApplication<CentsibleExportApplication>(*args)
}

@Component
class ExportStartupBanner(private val workerProperties: WorkerProperties) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun onReady() {
        log.info(
            "Centsible export worker started workerId={} leaseTimeoutSeconds={}",
            workerProperties.id, workerProperties.leaseTimeoutSeconds,
        )
    }
}
