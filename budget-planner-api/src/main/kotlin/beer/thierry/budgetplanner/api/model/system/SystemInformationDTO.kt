package beer.thierry.budgetplanner.api.model.system

import java.time.LocalDate

data class SystemInformationDTO(
    var name: String = "",
    var version: String = "",
    var description: String? = null,
    var license: String? = null,
    var repository: String? = null,
    var releasedAt: LocalDate? = null,
)
