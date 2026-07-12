package beer.thierry.centsible.core.services.account

import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.centsible.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.centsible.api.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.centsible.api.model.budgetaccount.UpdateBudgetAccountRequest
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountHistoryRepository
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.services.account.IBudgetAccountService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class BudgetAccountService(
    private val accountRepository: IBudgetAccountsRepository,
    private val accountHistoryRepository: IBudgetAccountHistoryRepository
) : IBudgetAccountService {

    private val log = LoggerFactory.getLogger(BudgetAccountService::class.java)

    @Transactional
    override fun createAccount(
        createBudgetAccountRequest: CreateBudgetAccountRequest,
        authenticatedUser: UserDTO,
    ): BudgetAccountDTO {
        val created = accountRepository.createAccount(authenticatedUser, createBudgetAccountRequest)
        log.info(
            "Created budget account id={} userId={} currency={}",
            created.id, authenticatedUser.id, created.currency,
        )
        return created
    }

    @Transactional
    override fun updateAccount(
        id: String,
        updateBudgetAccountRequest: UpdateBudgetAccountRequest,
        authenticatedUser: UserDTO,
    ): BudgetAccountDTO {
        val updated = accountRepository.updateAccount(UUID.fromString(id), updateBudgetAccountRequest, authenticatedUser)
        log.info("Updated budget account id={} userId={}", updated.id, authenticatedUser.id)
        return updated
    }

    @Transactional
    override fun deleteAccount(id: String, authenticatedUser: UserDTO) {
        accountRepository.deleteAccount(UUID.fromString(id), authenticatedUser)
        log.info("Deleted budget account id={} userId={}", id, authenticatedUser.id)
    }

    override fun fetchAccounts(authenticatedUser: UserDTO): List<BudgetAccountDTO> =
        accountRepository.fetchAllAccounts(authenticatedUser)

    override fun fetchAccountById(id: String, authenticatedUser: UserDTO): BudgetAccountDTO =
        accountRepository.fetchAccountById(UUID.fromString(id), authenticatedUser)

    override fun fetchAccountSnapshots(
        accountId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        authenticatedUser: UserDTO
    ): List<BudgetAccountSnapshotDTO> {
        val uuid = UUID.fromString(accountId)
        val startDateOffset = OffsetDateTime.of(startDate, LocalTime.MIN, ZoneOffset.UTC)
        val endDateOffset = OffsetDateTime.of(endDate, LocalTime.MAX, ZoneOffset.UTC)

        require(startDateOffset.isBefore(endDateOffset)) { "Start date must be before end date" }

        return accountHistoryRepository.fetchAccountHistory(uuid, startDateOffset, endDateOffset, authenticatedUser)
    }
}
