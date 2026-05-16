package beer.thierry.centsible.api.model.contact

import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class ContactDTO(
    var id: UUID? = null,
    var firstName: String = "",
    var lastName: String? = null,
    var name: String = "",
    var picture: String? = null,
    var totalLent: BigDecimal = BigDecimal.ZERO,
    var totalOwed: BigDecimal = BigDecimal.ZERO,
    var totalRepaid: BigDecimal = BigDecimal.ZERO,
    var outstanding: BigDecimal = BigDecimal.ZERO,
    var openLoanCount: Long = 0,
    var lastActivityAt: OffsetDateTime? = null,
    var createdAt: OffsetDateTime? = null,
)
