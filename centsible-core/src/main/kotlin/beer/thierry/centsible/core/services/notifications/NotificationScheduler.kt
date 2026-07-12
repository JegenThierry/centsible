package beer.thierry.centsible.core.services.notifications

import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.notifications.INotificationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Sweeps all users on a slow cadence (every 6 hours by default) to emit non-transaction-driven
 * notifications: upcoming/overdue loans, upcoming recurring rules, low-balance alerts.
 */
@Component
class NotificationScheduler(
    private val notificationService: INotificationService,
    private val userRepository: IUserRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(
        initialDelayString = "\${notifications.scheduler.initial-delay-ms:60000}",
        fixedDelayString = "\${notifications.scheduler.fixed-delay-ms:21600000}",
    )
    fun sweep() {
        val ids = runCatching { userRepository.fetchAllUserIds() }
            .getOrElse {
                log.warn("Failed to enumerate users for notification sweep", it)
                return
            }
        for (id in ids) {
            val proxy = UserDTO(id = id)
            try {
                notificationService.runScheduledChecks(proxy)
            } catch (e: Exception) {
                log.warn("Notification sweep failed for user {}", id, e)
            }
        }
    }
}
