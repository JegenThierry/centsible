package beer.thierry.centsible.api.services.reports

import beer.thierry.centsible.api.model.reports.SafeToSpendDTO
import beer.thierry.centsible.api.model.user.UserDTO

/** Computes the discretionary "safe to spend" figure for the current month, scoped to the user (ADR-0003). */
interface ISafeToSpendService {
    fun fetchForCurrentMonth(authenticatedUser: UserDTO): SafeToSpendDTO
}
