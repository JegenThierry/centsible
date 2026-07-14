package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.contact.ContactDTO
import beer.thierry.centsible.api.model.loan.IouShareForm
import beer.thierry.centsible.api.model.loan.LoanDTO
import beer.thierry.centsible.api.model.loan.LoanForm
import beer.thierry.centsible.api.model.loan.LoanUpdateForm
import beer.thierry.centsible.api.model.loan.RepaymentDTO
import beer.thierry.centsible.api.model.loan.RepaymentForm
import beer.thierry.centsible.api.model.loan.SplitToLoansForm
import beer.thierry.centsible.api.model.transaction.TransactionDTO
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.IContactsRepository
import beer.thierry.centsible.api.repository.ILoanRepaymentsRepository
import beer.thierry.centsible.api.repository.ILoansRepository
import beer.thierry.centsible.api.repository.ITransactionRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.currency.ICurrencyConversionService
import beer.thierry.centsible.core.services.loans.LoanService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class LoanServiceTest {

    private fun <T> anyArg(): T = org.mockito.ArgumentMatchers.any()

    @Mock private lateinit var loansRepository: ILoansRepository
    @Mock private lateinit var repaymentsRepository: ILoanRepaymentsRepository
    @Mock private lateinit var contactsRepository: IContactsRepository
    @Mock private lateinit var budgetAccountsRepository: IBudgetAccountsRepository
    @Mock private lateinit var currencyConversionService: ICurrencyConversionService
    @Mock private lateinit var userRepository: IUserRepository
    @Mock private lateinit var transactionRepository: ITransactionRepository

    @InjectMocks
    private lateinit var service: LoanService

    private val user = UserDTO(UUID.randomUUID(), "u", "u@x", "U", "X", "U X", null)

    @Test
    fun `createLoan with existing contactId resolves through contactsRepository`() {
        val contactId = UUID.randomUUID()
        val form = LoanForm(
            contactId = contactId,
            affectBalance = false,
            lentAmount = BigDecimal("100.00"),
            owedAmount = BigDecimal("100.00"),
            description = "lunch",
        )
        val created = LoanDTO(id = UUID.randomUUID())

        `when`(contactsRepository.fetchContactById(user, contactId))
            .thenReturn(ContactDTO(id = contactId))
        `when`(loansRepository.createLoan(eqArg(user), eqArg(contactId), eqArg(form), anyArg(), anyArg()))
            .thenReturn(created)

        assertEquals(created, service.createLoan(user, form))
        verify(contactsRepository, never()).createContact(anyArg(), anyArg())
    }

    @Test
    fun `createLoan rejects when contactId references a non-existent contact`() {
        val contactId = UUID.randomUUID()
        val form = LoanForm(contactId = contactId, affectBalance = false, description = "lunch")

        `when`(contactsRepository.fetchContactById(user, contactId)).thenReturn(null)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.createLoan(user, form)
        }
        assertTrue(ex.message!!.contains("Contact"))
        verify(loansRepository, never()).createLoan(anyArg(), anyArg(), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `createLoan rejects when both contactId and newContact name are supplied`() {
        val form = LoanForm(
            contactId = UUID.randomUUID(),
            newContactFirstName = "Jane",
            affectBalance = false,
            description = "lunch",
        )

        assertThrows(IllegalArgumentException::class.java) {
            service.createLoan(user, form)
        }
        verify(loansRepository, never()).createLoan(anyArg(), anyArg(), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `createLoan rejects when neither contactId nor a new contact name is provided`() {
        val form = LoanForm(affectBalance = false, description = "lunch")

        assertThrows(IllegalArgumentException::class.java) {
            service.createLoan(user, form)
        }
    }

    @Test
    fun `createLoan creates a new contact when only the new-contact name is provided`() {
        val newContactId = UUID.randomUUID()
        val form = LoanForm(
            newContactFirstName = "Jane",
            newContactLastName = "Doe",
            affectBalance = false,
            description = "lunch",
        )
        val created = LoanDTO(id = UUID.randomUUID())

        `when`(contactsRepository.createContact(eqArg(user), anyArg()))
            .thenReturn(ContactDTO(id = newContactId, firstName = "Jane", lastName = "Doe"))
        `when`(loansRepository.createLoan(eqArg(user), eqArg(newContactId), eqArg(form), anyArg(), anyArg()))
            .thenReturn(created)

        assertEquals(created, service.createLoan(user, form))
    }

    @Test
    fun `createLoan with an interest rate computes the owed amount from the lent amount`() {
        val contactId = UUID.randomUUID()
        val form = LoanForm(
            contactId = contactId,
            affectBalance = false,
            lentAmount = BigDecimal("100.00"),
            owedAmount = BigDecimal("100.00"),
            interestRate = BigDecimal("10"),
            description = "lunch",
        )

        `when`(contactsRepository.fetchContactById(user, contactId)).thenReturn(ContactDTO(id = contactId))
        `when`(loansRepository.createLoan(eqArg(user), eqArg(contactId), eqArg(form), anyArg(), anyArg()))
            .thenReturn(LoanDTO(id = UUID.randomUUID()))

        service.createLoan(user, form)

        assertEquals(BigDecimal("110.00"), form.owedAmount)
    }

    @Test
    fun `updateLoan recomputes owed from interest against the existing lent amount`() {
        val loanId = UUID.randomUUID()
        val existing = LoanDTO(id = loanId, lentAmount = BigDecimal("200.00"), totalRepaid = BigDecimal.ZERO)
        val form = LoanUpdateForm(description = "x", owedAmount = BigDecimal.ZERO, interestRate = BigDecimal("5"))

        `when`(loansRepository.fetchLoanById(user, loanId)).thenReturn(existing)
        `when`(loansRepository.updateLoan(eqArg(user), eqArg(loanId), eqArg(form))).thenReturn(LoanDTO(id = loanId))

        service.updateLoan(user, loanId, form)

        assertEquals(BigDecimal("210.00"), form.owedAmount)
    }

    @Test
    fun `updateLoan rejects an owed amount below what has already been repaid`() {
        val loanId = UUID.randomUUID()
        val existing = LoanDTO(id = loanId, lentAmount = BigDecimal("100.00"), totalRepaid = BigDecimal("40.00"))
        val form = LoanUpdateForm(description = "x", owedAmount = BigDecimal("30.00"))

        `when`(loansRepository.fetchLoanById(user, loanId)).thenReturn(existing)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.updateLoan(user, loanId, form)
        }
        assertTrue(ex.message!!.contains("repaid"))
        verify(loansRepository, never()).updateLoan(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `recordRepayment rejects when the loan does not exist`() {
        val loanId = UUID.randomUUID()
        `when`(loansRepository.fetchLoanById(user, loanId)).thenReturn(null)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.recordRepayment(user, loanId, RepaymentForm(amount = BigDecimal("10.00")))
        }
        assertTrue(ex.message!!.contains("Loan"))
    }

    @Test
    fun `recordRepayment rejects when the amount exceeds the outstanding balance`() {
        val loanId = UUID.randomUUID()
        `when`(loansRepository.fetchLoanById(user, loanId))
            .thenReturn(LoanDTO(id = loanId, owedAmount = BigDecimal("50.00")))
        `when`(repaymentsRepository.totalRepaidForLoan(loanId)).thenReturn(BigDecimal("30.00"))

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.recordRepayment(user, loanId, RepaymentForm(amount = BigDecimal("25.00")))
        }
        assertTrue(ex.message!!.contains("exceeds"))
        verify(repaymentsRepository, never()).createRepayment(anyArg(), anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `recordRepayment accepts an amount equal to outstanding (settles the loan)`() {
        val loanId = UUID.randomUUID()
        val form = RepaymentForm(affectBalance = false, amount = BigDecimal("20.00"))
        val repayment = RepaymentDTO(id = UUID.randomUUID())

        `when`(loansRepository.fetchLoanById(user, loanId))
            .thenReturn(LoanDTO(id = loanId, owedAmount = BigDecimal("50.00")))
        `when`(repaymentsRepository.totalRepaidForLoan(loanId)).thenReturn(BigDecimal("30.00"))
        `when`(repaymentsRepository.createRepayment(eqArg(user), eqArg(loanId), eqArg(form), anyArg()))
            .thenReturn(repayment)

        assertEquals(repayment, service.recordRepayment(user, loanId, form))
    }

    @Test
    fun `splitTransactionIntoLoans creates one IOU per share for an expense`() {
        val txId = UUID.randomUUID()
        val contactA = UUID.randomUUID()
        val contactB = UUID.randomUUID()
        val tx = TransactionDTO(
            id = txId, type = CategoryType.EXPENSE, amount = BigDecimal("120.00"), description = "Dinner",
        )
        val form = SplitToLoansForm(
            currency = Currency.EUR,
            shares = listOf(
                IouShareForm(contactId = contactA, amount = BigDecimal("40.00")),
                IouShareForm(contactId = contactB, amount = BigDecimal("40.00")),
            ),
        )
        `when`(transactionRepository.fetchTransactionById(txId, user)).thenReturn(tx)
        `when`(contactsRepository.fetchContactById(user, contactA)).thenReturn(ContactDTO(id = contactA))
        `when`(contactsRepository.fetchContactById(user, contactB)).thenReturn(ContactDTO(id = contactB))
        `when`(
            loansRepository.createIouLoan(
                anyArg(), anyArg(), anyArg(), anyArg(), anyArg(), anyArg(), anyArg(), anyArg(), anyArg(),
            ),
        ).thenReturn(LoanDTO(id = UUID.randomUUID()))

        val result = service.splitTransactionIntoLoans(user, txId, form)

        assertEquals(2, result.size)
        verify(loansRepository, times(2)).createIouLoan(
            anyArg(), anyArg(), anyArg(), anyArg(), anyArg(), anyArg(), anyArg(), anyArg(), anyArg(),
        )
    }

    @Test
    fun `splitTransactionIntoLoans rejects when shares exceed the transaction amount`() {
        val txId = UUID.randomUUID()
        val tx = TransactionDTO(id = txId, type = CategoryType.EXPENSE, amount = BigDecimal("50.00"))
        `when`(transactionRepository.fetchTransactionById(txId, user)).thenReturn(tx)

        val form = SplitToLoansForm(
            shares = listOf(IouShareForm(contactId = UUID.randomUUID(), amount = BigDecimal("80.00"))),
        )
        assertThrows(LocalizedException.BadRequest::class.java) {
            service.splitTransactionIntoLoans(user, txId, form)
        }
        verify(loansRepository, never()).createIouLoan(
            anyArg(), anyArg(), anyArg(), anyArg(), anyArg(), anyArg(), anyArg(), anyArg(), anyArg(),
        )
    }

    @Test
    fun `splitTransactionIntoLoans rejects a non-expense transaction`() {
        val txId = UUID.randomUUID()
        val tx = TransactionDTO(id = txId, type = CategoryType.INCOME, amount = BigDecimal("50.00"))
        `when`(transactionRepository.fetchTransactionById(txId, user)).thenReturn(tx)

        val form = SplitToLoansForm(
            shares = listOf(IouShareForm(contactId = UUID.randomUUID(), amount = BigDecimal("10.00"))),
        )
        assertThrows(LocalizedException.BadRequest::class.java) {
            service.splitTransactionIntoLoans(user, txId, form)
        }
    }

    private fun <T> eqArg(value: T): T = org.mockito.ArgumentMatchers.eq(value) ?: value
}
