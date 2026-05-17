package beer.thierry.centsible.core.services.budget

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budget.BudgetForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetRepository
import beer.thierry.centsible.api.services.budget.IBudgetService
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
