package beer.thierry.budgetplanner.core.services.budget

import beer.thierry.budgetplanner.api.model.budget.BudgetDTO
import beer.thierry.budgetplanner.api.model.budget.BudgetForm
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.repository.IBudgetRepository
import beer.thierry.budgetplanner.api.services.budget.IBudgetService
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.*

@Service
class BudgetService(
    private val repository: IBudgetRepository,
) : IBudgetService {

    override fun fetchAllForMonth(authenticatedUser: UserDTO, yearMonth: YearMonth): List<BudgetDTO> =
        repository.fetchAllWithSpentForMonth(authenticatedUser, yearMonth)

    override fun create(form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO =
        repository.create(form, authenticatedUser)

    override fun update(id: UUID, form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO =
        repository.update(id, form, authenticatedUser)

    override fun delete(id: UUID, authenticatedUser: UserDTO) {
        repository.delete(id, authenticatedUser)
    }
}
