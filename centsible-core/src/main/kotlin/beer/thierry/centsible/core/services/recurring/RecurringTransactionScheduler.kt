package beer.thierry.centsible.core.services.recurring

import beer.thierry.centsible.api.services.recurring.IRecurringTransactionService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RecurringTransactionScheduler(private val service: IRecurringTransactionService) {

    private val log = LoggerFactory.getLogger(RecurringTransactionScheduler::class.java)

    @Scheduled(
        cron = "\${recurring.materialization.cron:0 5 0 * * *}",
        zone = "\${TZ:UTC}",
    )
    fun materializeDue() {
        try {
            service.runMaterializationPass()
        } catch (e: Exception) {
            log.error("Recurring materialization pass failed; will retry on next tick", e)
        }
    }
}
