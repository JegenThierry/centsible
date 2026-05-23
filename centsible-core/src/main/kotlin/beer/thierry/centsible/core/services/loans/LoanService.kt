package beer.thierry.centsible.core.services.loans

import beer.thierry.centsible.api.model.contact.ContactForm
import beer.thierry.centsible.api.model.loan.LoanDTO
import beer.thierry.centsible.api.model.loan.LoanForm
import beer.thierry.centsible.api.model.loan.RepaymentDTO
import beer.thierry.centsible.api.model.loan.RepaymentForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IContactsRepository
import beer.thierry.centsible.api.repository.ILoanRepaymentsRepository
import beer.thierry.centsible.api.repository.ILoansRepository
import beer.thierry.centsible.api.services.loans.ILoanService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class LoanService(
    private val loansRepository: ILoansRepository,
    private val loanRepaymentsRepository: ILoanRepaymentsRepository,
    private val contactsRepository: IContactsRepository,
) : ILoanService {

    private val log = LoggerFactory.getLogger(LoanService::class.java)

    override fun fetchAllLoans(authenticatedUser: UserDTO): List<LoanDTO> =
        loansRepository.fetchAllLoans(authenticatedUser)

    override fun fetchLoansByContact(authenticatedUser: UserDTO, contactId: UUID): List<LoanDTO> =
        loansRepository.fetchLoansByContact(authenticatedUser, contactId)

    override fun fetchLoanById(authenticatedUser: UserDTO, id: UUID): LoanDTO? =
        loansRepository.fetchLoanById(authenticatedUser, id)

    @Transactional
    override fun createLoan(authenticatedUser: UserDTO, form: LoanForm): LoanDTO {
        val contactId = resolveContactId(authenticatedUser, form)
        val created = loansRepository.createLoan(authenticatedUser, contactId, form)
        log.info(
            "Created loan id={} userId={} contactId={} owedAmount={}",
            created.id, authenticatedUser.id, contactId, form.owedAmount,
        )
        return created
    }

    @Transactional
    override fun deleteLoan(authenticatedUser: UserDTO, id: UUID): Boolean {
        val deleted = loansRepository.deleteLoan(authenticatedUser, id)
        if (deleted) {
            log.info("Deleted loan id={} userId={}", id, authenticatedUser.id)
        }
        return deleted
    }

    override fun fetchRepayments(authenticatedUser: UserDTO, loanId: UUID): List<RepaymentDTO> =
        loanRepaymentsRepository.fetchRepayments(authenticatedUser, loanId)

    @Transactional
    override fun recordRepayment(authenticatedUser: UserDTO, loanId: UUID, form: RepaymentForm): RepaymentDTO {
        val loan = loansRepository.fetchLoanById(authenticatedUser, loanId)
            ?: throw IllegalArgumentException("Loan not found.")
        val outstanding = loan.owedAmount - loanRepaymentsRepository.totalRepaidForLoan(loanId)
        if (form.amount > outstanding) {
            throw IllegalArgumentException(
                "Repayment amount ($${form.amount}) exceeds the outstanding balance ($outstanding)."
            )
        }
        val repayment = loanRepaymentsRepository.createRepayment(authenticatedUser, loanId, form)
        log.info(
            "Recorded loan repayment id={} loanId={} userId={} amount={}",
            repayment.id, loanId, authenticatedUser.id, form.amount,
        )
        return repayment
    }

    @Transactional
    override fun deleteRepayment(authenticatedUser: UserDTO, loanId: UUID, repaymentId: UUID): Boolean {
        val deleted = loanRepaymentsRepository.deleteRepayment(authenticatedUser, loanId, repaymentId)
        if (deleted) {
            log.info(
                "Deleted loan repayment id={} loanId={} userId={}",
                repaymentId, loanId, authenticatedUser.id,
            )
        }
        return deleted
    }

    override fun totalOutstanding(authenticatedUser: UserDTO): BigDecimal =
        loansRepository.totalOutstanding(authenticatedUser)

    private fun resolveContactId(authenticatedUser: UserDTO, form: LoanForm): UUID {
        val existing = form.contactId
        val newFirstName = form.newContactFirstName?.takeIf { it.isNotBlank() }

        require(existing == null || newFirstName == null) {
            "Provide either an existing contact or a new contact name, not both."
        }
        if (existing != null) {
            contactsRepository.fetchContactById(authenticatedUser, existing)
                ?: throw IllegalArgumentException("Contact not found.")
            return existing
        }
        requireNotNull(newFirstName) { "Either contactId or newContactFirstName is required." }
        val created = contactsRepository.createContact(
            authenticatedUser,
            ContactForm(firstName = newFirstName, lastName = form.newContactLastName?.takeIf { it.isNotBlank() })
        )
        return created.id ?: throw IllegalStateException("Failed to create contact.")
    }
}
