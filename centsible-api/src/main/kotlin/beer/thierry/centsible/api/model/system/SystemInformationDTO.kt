package beer.thierry.centsible.api.model.system

import java.time.LocalDate

data class SystemInformationDTO(
    val name: String = "",
    val version: String = "",
    val description: String? = null,
    val license: String? = null,
    val repository: String? = null,
    val releasedAt: LocalDate? = null,
)
