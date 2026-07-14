package beer.thierry.centsible.api.model.export

import beer.thierry.centsible.api.model.recurring.Frequency
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

/** Creates a recurring export. v1 always exports the user's transactions across all accounts. */
data class ExportScheduleForm(
    @field:NotBlank(message = "{validation.export.schedule.title.required}")
    @field:Size(max = 200, message = "{validation.export.schedule.title.tooLong}")
    var title: String = "",

    @field:NotNull(message = "{validation.export.schedule.format.required}")
    var format: ExportFormat = ExportFormat.PDF,

    @field:NotNull(message = "{validation.export.schedule.frequency.required}")
    var frequency: Frequency? = null,
)

/** Edits a schedule, including pausing/resuming it via [active]. */
data class ExportScheduleUpdateForm(
    @field:NotBlank(message = "{validation.export.schedule.title.required}")
    @field:Size(max = 200, message = "{validation.export.schedule.title.tooLong}")
    var title: String = "",

    @field:NotNull(message = "{validation.export.schedule.format.required}")
    var format: ExportFormat = ExportFormat.PDF,

    @field:NotNull(message = "{validation.export.schedule.frequency.required}")
    var frequency: Frequency? = null,

    var active: Boolean = true,
)
