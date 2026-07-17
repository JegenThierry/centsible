package beer.thierry.centsiblerest.resources.export

import beer.thierry.centsible.api.model.export.ExportScheduleDTO
import beer.thierry.centsible.api.model.export.ExportType
import beer.thierry.centsible.api.model.export.PostProcessingType
import beer.thierry.centsible.api.model.recurring.Frequency
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.account.IBudgetAccountService
import beer.thierry.centsible.api.services.export.IExportScheduleService
import beer.thierry.centsible.api.services.export.IExportService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * Turns due [ExportScheduleDTO]s into ordinary export jobs. Lives in `rest` (not `core`) because it
 * reuses [ExportProtoBuilder] to serialize the payload — the same path [ExportResource] uses for
 * one-off exports — so a scheduled run is indistinguishable from a manual one to the worker.
 */
@Component
class ExportScheduleMaterializer(
    private val scheduleService: IExportScheduleService,
    private val exportService: IExportService,
    private val protoBuilder: ExportProtoBuilder,
    private val budgetAccountService: IBudgetAccountService,
    private val userRepository: IUserRepository,
) {
    private val log = LoggerFactory.getLogger(ExportScheduleMaterializer::class.java)

    @Scheduled(
        cron = "\${export.schedules.cron:0 15 0 * * *}",
        zone = "\${TZ:UTC}",
    )
    fun materializeDue() {
        val today = LocalDate.now()
        val due = try {
            scheduleService.fetchDue(today)
        } catch (e: Exception) {
            log.error("Failed to fetch due export schedules; will retry on next tick", e)
            return
        }
        for (schedule in due) {
            try {
                runSchedule(schedule)
            } catch (e: Exception) {
                log.error("Export schedule {} failed to materialize", schedule.id, e)
            }
        }
    }

    private fun runSchedule(schedule: ExportScheduleDTO) {
        val scheduleId = schedule.id ?: return
        val userId = schedule.userId ?: return
        val frequency = schedule.frequency ?: return
        val nextRunAt = schedule.nextRunAt ?: return

        val user = userRepository.findUserById(userId)?.let {
            UserDTO(
                id = it.id,
                username = it.username,
                email = it.email,
                firstName = it.firstName,
                lastName = it.lastName,
                locale = it.locale,
                defaultCurrency = it.defaultCurrency,
            )
        } ?: run {
            log.warn("Export schedule {} references missing user {}", scheduleId, userId)
            return
        }

        // Advance once per pass: if several periods were missed the next daily tick picks up the rest.
        val advanceTo = frequency.advance(nextRunAt)
        // Claim the run by advancing the schedule *before* enqueueing: fetchDue takes no lock, so this
        // CAS is what stops a second rest instance on the same cron from emailing the user a duplicate
        // export. A crash after this point costs the user one period rather than re-emailing every tick.
        if (!scheduleService.markRun(scheduleId, nextRunAt, advanceTo)) {
            log.debug("Export schedule {} already claimed for {}", scheduleId, nextRunAt)
            return
        }

        val accounts = budgetAccountService.fetchAccounts(user)
        if (accounts.isEmpty()) return

        val (windowStart, windowEnd) = windowFor(frequency, nextRunAt)
        val params = TransactionsExportParams(
            accountIds = accounts.map { it.id },
            fromDate = windowStart,
            toDate = windowEnd,
            categoryIds = emptyList(),
        )
        val payload = protoBuilder.build(
            user = user,
            params = params,
            locale = user.locale,
            currency = primaryCurrencyOf(accounts.map { it.currency }),
            format = schedule.format,
        )
        exportService.create(
            user,
            ExportType.TRANSACTIONS,
            schedule.title,
            payload,
            listOf(PostProcessingType.SEND_EMAIL to emptyMap()),
        )
        log.info(
            "Materialized export schedule id={} userId={} format={} window={}..{}",
            scheduleId, userId, schedule.format, windowStart, windowEnd,
        )
    }

    /** The just-completed period ending the day before [nextRunAt], sized to [frequency]. */
    private fun windowFor(frequency: Frequency, nextRunAt: LocalDate): Pair<LocalDate, LocalDate> {
        val end = nextRunAt.minusDays(1)
        val start = when (frequency) {
            Frequency.DAILY -> nextRunAt.minusDays(1)
            Frequency.WEEKLY -> nextRunAt.minusWeeks(1)
            Frequency.MONTHLY -> nextRunAt.minusMonths(1)
            Frequency.YEARLY -> nextRunAt.minusYears(1)
        }
        return start to end
    }
}
