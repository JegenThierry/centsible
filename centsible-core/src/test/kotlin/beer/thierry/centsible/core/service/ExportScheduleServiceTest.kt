package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.export.ExportFormat
import beer.thierry.centsible.api.model.export.ExportScheduleDTO
import beer.thierry.centsible.api.model.export.ExportScheduleForm
import beer.thierry.centsible.api.model.recurring.Frequency
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IExportScheduleRepository
import beer.thierry.centsible.core.services.export.ExportScheduleService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ExportScheduleServiceTest {

    private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()

    @Mock private lateinit var repository: IExportScheduleRepository

    @InjectMocks private lateinit var service: ExportScheduleService

    private val user = UserDTO(id = UUID.randomUUID())

    /** Stubs repository.create and returns the next-run-at (arg index 5) it was called with. */
    private fun captureNextRunOnCreate(): () -> LocalDate? {
        var captured: LocalDate? = null
        `when`(repository.create(anyArg(), anyArg(), anyArg(), anyArg(), anyArg(), anyArg())).thenAnswer {
            captured = it.getArgument<LocalDate>(5)
            ExportScheduleDTO(id = UUID.randomUUID())
        }
        return { captured }
    }

    @Test
    fun `create aligns a monthly schedule to the first of the next month`() {
        val nextRun = captureNextRunOnCreate()

        service.create(user, ExportScheduleForm(title = "Monthly statement", format = ExportFormat.PDF, frequency = Frequency.MONTHLY))

        val date = requireNotNull(nextRun())
        assertEquals(1, date.dayOfMonth)
        assertTrue(date.isAfter(LocalDate.now()))
    }

    @Test
    fun `create aligns a weekly schedule to the next Monday`() {
        val nextRun = captureNextRunOnCreate()

        service.create(user, ExportScheduleForm(title = "Weekly CSV", format = ExportFormat.CSV, frequency = Frequency.WEEKLY))

        val date = requireNotNull(nextRun())
        assertEquals(DayOfWeek.MONDAY, date.dayOfWeek)
        assertTrue(date.isAfter(LocalDate.now()))
    }

    @Test
    fun `create rejects a missing frequency`() {
        assertThrows(LocalizedException.BadRequest::class.java) {
            service.create(user, ExportScheduleForm(title = "x", format = ExportFormat.PDF, frequency = null))
        }
    }
}
