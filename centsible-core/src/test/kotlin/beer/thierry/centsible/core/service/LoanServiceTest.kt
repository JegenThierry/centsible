package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.model.contact.ContactDTO
import beer.thierry.centsible.api.model.loan.LoanDTO
import beer.thierry.centsible.api.model.loan.LoanForm
import beer.thierry.centsible.api.model.loan.RepaymentDTO
import beer.thierry.centsible.api.model.loan.RepaymentForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IContactsRepository
import beer.thierry.centsible.api.repository.ILoanRepaymentsRepository
import beer.thierry.centsible.api.repository.ILoansRepository
import beer.thierry.centsible.core.services.loans.LoanService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
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

    @InjectMocks
    private lateinit var service: LoanService

    private val user = UserDTO(UUID.randomUUID(), "u", "u@x", "U", "X", "U X", null)

    @Test
    fun `createLoan with existing contactId resolves through contactsRepository`() {
        val contactId = UUID.randomUUID()
        val form = LoanForm(
            contactId = contactId,
            lentAmount = BigDecimal("100.00"),
            owedAmount = BigDecimal("100.00"),
            description = "lunch",
        )
        val created = LoanDTO(id = UUID.randomUUID())

        `when`(contactsRepository.fetchContactById(user, contactId))
            .thenReturn(ContactDTO(id = contactId))
        `when`(loansRepository.createLoan(user, contactId, form)).thenReturn(created)

        assertEquals(created, service.createLoan(user, form))
        verify(contactsRepository, never()).createContact(anyArg(), anyArg())
    }

    @Test
    fun `createLoan rejects when contactId references a non-existent contact`() {
        val contactId = UUID.randomUUID()
        val form = LoanForm(contactId = contactId, description = "lunch")

        `when`(contactsRepository.fetchContactById(user, contactId)).thenReturn(null)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.createLoan(user, form)
        }
        assertTrue(ex.message!!.contains("Contact"))
        verify(loansRepository, never()).createLoan(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `createLoan rejects when both contactId and newContact name are supplied`() {
        val form = LoanForm(
            contactId = UUID.randomUUID(),
            newContactFirstName = "Jane",
            description = "lunch",
        )

        assertThrows(IllegalArgumentException::class.java) {
            service.createLoan(user, form)
        }
        verify(loansRepository, never()).createLoan(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `createLoan rejects when neither contactId nor a new contact name is provided`() {
        val form = LoanForm(description = "lunch")

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
            description = "lunch",
        )
        val created = LoanDTO(id = UUID.randomUUID())

        `when`(contactsRepository.createContact(eqArg(user), anyArg()))
            .thenReturn(ContactDTO(id = newContactId, firstName = "Jane", lastName = "Doe"))
        `when`(loansRepository.createLoan(user, newContactId, form)).thenReturn(created)

        assertEquals(created, service.createLoan(user, form))
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

        // Outstanding = 50 - 30 = 20. Try to repay 25.
        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.recordRepayment(user, loanId, RepaymentForm(amount = BigDecimal("25.00")))
        }
        assertTrue(ex.message!!.contains("exceeds"))
        verify(repaymentsRepository, never()).createRepayment(anyArg(), anyArg(), anyArg())
    }

    @Test
    fun `recordRepayment accepts an amount equal to outstanding (settles the loan)`() {
        val loanId = UUID.randomUUID()
        val form = RepaymentForm(amount = BigDecimal("20.00"))
        val repayment = RepaymentDTO(id = UUID.randomUUID())

        `when`(loansRepository.fetchLoanById(user, loanId))
            .thenReturn(LoanDTO(id = loanId, owedAmount = BigDecimal("50.00")))
        `when`(repaymentsRepository.totalRepaidForLoan(loanId)).thenReturn(BigDecimal("30.00"))
        `when`(repaymentsRepository.createRepayment(user, loanId, form)).thenReturn(repayment)

        assertEquals(repayment, service.recordRepayment(user, loanId, form))
    }

    private fun <T> eqArg(value: T): T = org.mockito.ArgumentMatchers.eq(value) ?: value
}
