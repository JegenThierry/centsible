package beer.thierry.centsible.api.services.budget

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budget.BudgetForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.time.YearMonth
import java.util.*

interface IBudgetService {
    fun fetchAllForMonth(authenticatedUser: UserDTO, yearMonth: YearMonth = YearMonth.now()): List<BudgetDTO>
    fun create(form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO
    fun update(id: UUID, form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO
    fun delete(id: UUID, authenticatedUser: UserDTO)
}
