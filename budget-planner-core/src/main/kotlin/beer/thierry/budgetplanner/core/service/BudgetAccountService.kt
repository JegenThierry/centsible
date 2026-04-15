package beer.thierry.budgetplanner.core.service

import beer.thierry.budgetplanner.api.service.account.IBudgetAccountService
import beer.thierry.budgetplanner.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.budgetplanner.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.budgetplanner.api.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.repository.IBudgetAccountHistoryRepository
import beer.thierry.budgetplanner.api.repository.IBudgetAccountsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class BudgetAccountService(
    private val accountRepository: IBudgetAccountsRepository,
    private val accountHistoryRepository: IBudgetAccountHistoryRepository
) : IBudgetAccountService {

    @Transactional
    override fun createAccount(
        createBudgetAccountRequest: CreateBudgetAccountRequest,
        authenticatedUser: UserDTO,
    ): BudgetAccountDTO {
        return accountRepository.createAccount(authenticatedUser, createBudgetAccountRequest)
    }

    override fun fetchAccounts(authenticatedUser: UserDTO): List<BudgetAccountDTO> {
        return accountRepository.fetchAllAccounts(authenticatedUser)
    }

    override fun fetchAccountById(id: String, authenticatedUser: UserDTO): BudgetAccountDTO {
        return accountRepository.fetchAccountById(UUID.fromString(id), authenticatedUser)
    }

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
