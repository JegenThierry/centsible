package beer.thierry.budgetplanner.core.service

import beer.thierry.budgetplanner.api.model.budgetaccount.BudgetAccountDTO
import beer.thierry.budgetplanner.api.model.budgetaccount.BudgetAccountSnapshotDTO
import beer.thierry.budgetplanner.api.model.budgetaccount.CreateBudgetAccountRequest
import beer.thierry.budgetplanner.api.model.budgetaccount.Currency
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.repository.IBudgetAccountHistoryRepository
import beer.thierry.budgetplanner.api.repository.IBudgetAccountsRepository
import beer.thierry.budgetplanner.core.services.account.BudgetAccountService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class BudgetAccountServiceTest {

    private fun <T> any(): T = org.mockito.ArgumentMatchers.any()
    private fun <T> eq(value: T): T = org.mockito.ArgumentMatchers.eq(value) ?: value

    @Mock
    private lateinit var accountRepository: IBudgetAccountsRepository

    @Mock
    private lateinit var accountHistoryRepository: IBudgetAccountHistoryRepository

    @InjectMocks
    private lateinit var service: BudgetAccountService

    @Test
    fun `createAccount should create account`() {
        val user = UserDTO(UUID.randomUUID(), "user", "user@example.com", "User", "Name", "User Name", null)
        val request = CreateBudgetAccountRequest("Main Account", BigDecimal("100.00"), Currency.EUR)
        val accountId = UUID.randomUUID()
        val account =
            BudgetAccountDTO(accountId, "Main Account", BigDecimal("100.00"), BigDecimal("100.00"), Currency.EUR)

        `when`(accountRepository.createAccount(user, request)).thenReturn(account)

        val result = service.createAccount(request, user)

        assertEquals(account, result)
        verify(accountRepository).createAccount(user, request)
    }

    @Test
    fun `fetchAccounts should return all accounts`() {
        val user = UserDTO(UUID.randomUUID(), "user", "user@example.com", "User", "Name", "User Name", null)
        val accounts = listOf(
            BudgetAccountDTO(UUID.randomUUID(), "Account 1", BigDecimal("10.00"), BigDecimal("10.00"), Currency.EUR),
            BudgetAccountDTO(UUID.randomUUID(), "Account 2", BigDecimal("20.00"), BigDecimal("10.00"), Currency.EUR)
        )

        `when`(accountRepository.fetchAllAccounts(user)).thenReturn(accounts)

        val result = service.fetchAccounts(user)

        assertEquals(accounts, result)
        verify(accountRepository).fetchAllAccounts(user)
    }

    @Test
    fun `fetchAccountById should return account by id`() {
        val user = UserDTO(UUID.randomUUID(), "user", "user@example.com", "User", "Name", "User Name", null)
        val accountId = UUID.randomUUID()
        val account =
            BudgetAccountDTO(accountId, "Main Account", BigDecimal("100.00"), BigDecimal("100.00"), Currency.EUR)

        `when`(accountRepository.fetchAccountById(accountId, user)).thenReturn(account)

        val result = service.fetchAccountById(accountId.toString(), user)

        assertEquals(account, result)
        verify(accountRepository).fetchAccountById(accountId, user)
    }

    @Test
    fun `fetchAccountSnapshots should return history for date range`() {
        val user = UserDTO(UUID.randomUUID(), "user", "user@example.com", "User", "Name", "User Name", null)
        val accountId = UUID.randomUUID()
        val startDate = LocalDate.now().minusDays(7)
        val endDate = LocalDate.now()
        val snapshots = listOf(
            BudgetAccountSnapshotDTO(1, accountId, BigDecimal("100.00"), OffsetDateTime.now())
        )

        `when`(
            accountHistoryRepository.fetchAccountHistory(
                eq(accountId),
                any(),
                any(),
                eq(user)
            )
        ).thenReturn(snapshots)

        val result = service.fetchAccountSnapshots(accountId.toString(), startDate, endDate, user)

        assertEquals(snapshots, result)
        verify(accountHistoryRepository).fetchAccountHistory(eq(accountId), any(), any(), eq(user))
    }
}
