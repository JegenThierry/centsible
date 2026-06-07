package beer.thierry.centsible.core.services.loans

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.contact.ContactForm
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.loan.LoanDTO
import beer.thierry.centsible.api.model.loan.LoanForm
import beer.thierry.centsible.api.model.loan.LoanUpdateForm
import beer.thierry.centsible.api.model.loan.RepaymentDTO
import beer.thierry.centsible.api.model.loan.RepaymentForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.IContactsRepository
import beer.thierry.centsible.api.repository.ILoanRepaymentsRepository
import beer.thierry.centsible.api.repository.ILoansRepository
import beer.thierry.centsible.api.repository.IUserRepository
import beer.thierry.centsible.api.services.currency.ICurrencyConversionService
import beer.thierry.centsible.api.services.loans.ILoanService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

@Service
class LoanService(
    private val loansRepository: ILoansRepository,
    private val loanRepaymentsRepository: ILoanRepaymentsRepository,
    private val contactsRepository: IContactsRepository,
    private val budgetAccountsRepository: IBudgetAccountsRepository,
    private val currencyConversionService: ICurrencyConversionService,
    private val userRepository: IUserRepository,
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

        // Resolve the loan currency: explicit choice, else the account currency (balance-affecting),
        // else the user's default currency (tracking-only).
        val loanCurrency: Currency = form.currency
            ?: form.accountId?.takeIf { form.affectBalance }?.let { budgetAccountsRepository.fetchAccountCurrency(it) }
            ?: parseCurrency(authenticatedUser.defaultCurrency)

        // Interest computes the owed amount once at creation.
        form.interestRate?.let { rate -> form.owedAmount = applyInterest(form.lentAmount, rate) }

        // FX for the lending transaction, only when the loan affects an account balance.
        val conversion: ConversionResult? = if (form.affectBalance) {
            val accountId = form.accountId
                ?: throw IllegalArgumentException("Account is required when the loan affects an account balance.")
            val accountCurrency = budgetAccountsRepository.fetchAccountCurrency(accountId)
            currencyConversionService.convert(form.lentAmount, loanCurrency, accountCurrency, form.transactionDate)
        } else null

        val created = loansRepository.createLoan(authenticatedUser, contactId, form, loanCurrency, conversion)
        log.info(
            "Created loan id={} userId={} contactId={} currency={} owedAmount={}",
            created.id, authenticatedUser.id, contactId, loanCurrency, form.owedAmount,
        )
        return created
    }

    @Transactional
    override fun updateLoan(authenticatedUser: UserDTO, id: UUID, form: LoanUpdateForm): LoanDTO? {
        val loan = loansRepository.fetchLoanById(authenticatedUser, id) ?: return null

        // Recompute owed from interest against the (immutable) lent amount; otherwise keep the provided value.
        form.interestRate?.let { rate -> form.owedAmount = applyInterest(loan.lentAmount, rate) }
        if (form.owedAmount < loan.totalRepaid) {
            throw IllegalArgumentException(
                "Owed amount (${form.owedAmount}) cannot be less than the amount already repaid (${loan.totalRepaid})."
            )
        }

        val updated = loansRepository.updateLoan(authenticatedUser, id, form)
        if (updated != null) {
            log.info("Updated loan id={} userId={} owedAmount={}", id, authenticatedUser.id, form.owedAmount)
        }
        return updated
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

        // The repayment is denominated in the loan currency; convert it into the account currency when
        // it affects a balance (no-op when they match).
        val conversion: ConversionResult? = if (form.affectBalance) {
            val accountId = form.accountId
                ?: throw IllegalArgumentException("Account is required when the repayment affects an account balance.")
            val accountCurrency = budgetAccountsRepository.fetchAccountCurrency(accountId)
            currencyConversionService.convert(form.amount, parseCurrency(loan.currency), accountCurrency, form.repaidAt)
        } else null

        val repayment = loanRepaymentsRepository.createRepayment(authenticatedUser, loanId, form, conversion)
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

    override fun totalOutstanding(authenticatedUser: UserDTO): BigDecimal {
        // Loans can be in different currencies, so convert each open balance into the user's default
        // currency before summing. FX failures skip that loan rather than failing the whole total.
        // The JWT principal doesn't carry the default currency, so read it from the repository.
        val defaultCurrency = parseCurrency(
            userRepository.findUserById(authenticatedUser.id)?.defaultCurrency ?: authenticatedUser.defaultCurrency
        )
        val today = LocalDate.now()
        return loansRepository.fetchAllLoans(authenticatedUser)
            .filter { it.outstanding.signum() > 0 }
            .fold(BigDecimal.ZERO) { acc, loan ->
                val converted = runCatching {
                    currencyConversionService
                        .convert(loan.outstanding, parseCurrency(loan.currency), defaultCurrency, today)
                        .convertedAmount
                }.getOrElse {
                    log.warn("Excluding loan {} ({}) from outstanding total: FX unavailable", loan.id, loan.currency, it)
                    BigDecimal.ZERO
                }
                acc + converted
            }
            .setScale(2, RoundingMode.HALF_UP)
    }

    /** owed = lent * (1 + rate/100), rounded to 2dp. */
    private fun applyInterest(lent: BigDecimal, ratePercent: BigDecimal): BigDecimal =
        lent.multiply(BigDecimal.ONE.add(ratePercent.movePointLeft(2))).setScale(2, RoundingMode.HALF_UP)

    private fun parseCurrency(code: String?): Currency =
        code?.let { runCatching { Currency.valueOf(it) }.getOrNull() } ?: Currency.EUR

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
