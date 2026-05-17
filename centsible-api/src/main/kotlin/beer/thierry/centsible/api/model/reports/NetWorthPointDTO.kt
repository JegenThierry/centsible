package beer.thierry.centsible.api.model.reports

import java.math.BigDecimal
import java.time.LocalDate

data class NetWorthPointDTO(
    var date: LocalDate = LocalDate.now(),
    var balance: BigDecimal = BigDecimal.ZERO,
)
