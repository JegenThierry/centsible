package beer.thierry.centsible.core.services.budget

import beer.thierry.centsible.api.model.budget.BudgetDTO
import beer.thierry.centsible.api.model.budget.BudgetForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetRepository
import beer.thierry.centsible.api.services.budget.IBudgetService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.*

@Service
class BudgetService(
    private val repository: IBudgetRepository,
) : IBudgetService {

    private val log = LoggerFactory.getLogger(BudgetService::class.java)

    override fun fetchAllForMonth(authenticatedUser: UserDTO, yearMonth: YearMonth): List<BudgetDTO> =
        repository.fetchAllWithSpentForMonth(authenticatedUser, yearMonth)

    override fun create(form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO {
        val created = repository.create(form, authenticatedUser)
        log.info(
            "Created budget id={} userId={} categoryId={} limit={}",
            created.id, authenticatedUser.id, form.categoryId, form.amountLimit,
        )
        return created
    }

    override fun update(id: UUID, form: BudgetForm, authenticatedUser: UserDTO): BudgetDTO {
        val updated = repository.update(id, form, authenticatedUser)
        log.info(
            "Updated budget id={} userId={} categoryId={} limit={}",
            id, authenticatedUser.id, form.categoryId, form.amountLimit,
        )
        return updated
    }

    override fun delete(id: UUID, authenticatedUser: UserDTO) {
        repository.delete(id, authenticatedUser)
        log.info("Deleted budget id={} userId={}", id, authenticatedUser.id)
    }
}
