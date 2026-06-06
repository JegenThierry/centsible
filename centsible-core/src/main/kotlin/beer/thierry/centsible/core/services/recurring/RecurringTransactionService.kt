package beer.thierry.centsible.core.services.recurring

import beer.thierry.centsible.api.model.recurring.RecurringTransactionDTO
import beer.thierry.centsible.api.model.recurring.RecurringTransactionForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.api.repository.IRecurringTransactionRepository
import beer.thierry.centsible.api.services.currency.ICurrencyConversionService
import beer.thierry.centsible.api.services.recurring.IRecurringTransactionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class RecurringTransactionService(
    private val repository: IRecurringTransactionRepository,
    private val categoriesRepository: ICategoriesRepository,
    private val accountRepository: IBudgetAccountsRepository,
    private val currencyConversionService: ICurrencyConversionService,
) : IRecurringTransactionService {

    private val log = LoggerFactory.getLogger(RecurringTransactionService::class.java)

    override fun fetchAll(authenticatedUser: UserDTO, accountId: UUID?): List<RecurringTransactionDTO> =
        repository.fetchAll(authenticatedUser, accountId)

    override fun create(
        accountId: UUID, form: RecurringTransactionForm, authenticatedUser: UserDTO
    ): RecurringTransactionDTO {
        val isTransfer = form.isTransfer || form.destinationAccountId != null
        validate(form, isTransfer)
        assertOwnership(authenticatedUser, sourceAccountId = accountId, form = form, isTransfer = isTransfer)
        val created = repository.create(accountId, form, authenticatedUser)
        log.info(
            "Created recurring transaction id={} accountId={} userId={} frequency={} transfer={}",
            created.id, accountId, authenticatedUser.id, form.frequency, isTransfer,
        )
        return created
    }

    override fun update(
        id: UUID, form: RecurringTransactionForm, authenticatedUser: UserDTO
    ): RecurringTransactionDTO {
        val isTransfer = form.isTransfer || form.destinationAccountId != null
        validate(form, isTransfer)
        if (isTransfer) {
            val existing = repository.fetchById(id, authenticatedUser)
            assertOwnership(authenticatedUser, sourceAccountId = existing.accountId, form = form, isTransfer = isTransfer)
        } else {
            assertCategoryOwned(authenticatedUser, form.categoryId!!)
        }
        val updated = repository.update(id, form, authenticatedUser)
        log.info(
            "Updated recurring transaction id={} userId={} frequency={} active={} transfer={}",
            id, authenticatedUser.id, form.frequency, form.active, isTransfer,
        )
        return updated
    }

    override fun delete(id: UUID, authenticatedUser: UserDTO) {
        repository.delete(id, authenticatedUser)
        log.info("Deleted recurring transaction id={} userId={}", id, authenticatedUser.id)
    }

    override fun setActive(id: UUID, active: Boolean, authenticatedUser: UserDTO): RecurringTransactionDTO {
        val current = repository.fetchById(id, authenticatedUser)
        val form = RecurringTransactionForm(
            amount = current.amount!!,
            categoryId = current.category.id,
            description = current.description!!,
            frequency = current.frequency!!,
            startDate = current.startDate!!,
            endDate = current.endDate,
            active = active,
            currency = current.originalCurrency,
            type = current.type,
            isTransfer = current.isTransfer,
            destinationAccountId = current.destinationAccountId,
        )
        val updated = repository.update(id, form, authenticatedUser)
        log.info(
            "Recurring transaction active={} id={} userId={}",
            active, id, authenticatedUser.id,
        )
        return updated
    }

    override fun runMaterializationPass(): Int {
        val today = LocalDate.now()
        var count = 0
        for (rule in repository.fetchDueRules(today)) {
            val isTransfer = rule.isTransfer || rule.destinationAccountId != null
            val accountId = rule.accountId ?: continue
            val accountCurrency = accountRepository.fetchAccountCurrency(accountId)
            val destinationCurrency = if (isTransfer) {
                val destId = rule.destinationAccountId ?: continue
                accountRepository.fetchAccountCurrency(destId)
            } else null
            var working = if (isTransfer && !rule.isTransfer) rule.copy(isTransfer = true) else rule
            while (working.active && (working.nextRunAt?.let { it <= today } == true)) {
                val occurrenceDate = working.nextRunAt!!
                val conversion = if (isTransfer) {
                    currencyConversionService.convert(
                        working.amount!!, accountCurrency, destinationCurrency!!, occurrenceDate,
                    )
                } else {
                    currencyConversionService.convert(
                        working.amount!!,
                        working.originalCurrency ?: accountCurrency,
                        accountCurrency,
                        occurrenceDate,
                    )
                }
                val newNext = repository.materializeOnce(working, conversion)
                count++
                if (newNext == null || newNext > today) break
                working = working.copy(nextRunAt = newNext)
            }
        }
        if (count > 0) log.info("Materialized {} recurring transaction(s) for {}", count, today)
        return count
    }

    private fun validate(form: RecurringTransactionForm, isTransfer: Boolean) {
        form.endDate?.let { end ->
            require(!end.isBefore(form.startDate)) { "End date must be on or after start date." }
        }
        if (isTransfer) {
            require(form.destinationAccountId != null) { "Destination account is required for a transfer." }
        } else {
            val categoryId = form.categoryId
            require(categoryId != null && categoryId > 0) { "Category is required." }
        }
    }

    private fun assertOwnership(
        authenticatedUser: UserDTO,
        sourceAccountId: UUID?,
        form: RecurringTransactionForm,
        isTransfer: Boolean,
    ) {
        if (isTransfer) {
            val destinationAccountId = form.destinationAccountId
                ?: throw IllegalArgumentException("Destination account is required for a transfer.")
            require(sourceAccountId != destinationAccountId) { "Source and destination accounts must be different." }
            accountRepository.fetchAccountById(destinationAccountId, authenticatedUser)
        } else {
            assertCategoryOwned(authenticatedUser, form.categoryId!!)
        }
    }

    private fun assertCategoryOwned(authenticatedUser: UserDTO, categoryId: Long) {
        if (!categoriesRepository.fetchCategoryClassifications(authenticatedUser, listOf(categoryId))
                .containsKey(categoryId)
        ) {
            throw IllegalArgumentException("Category $categoryId not found or not accessible")
        }
    }
}
