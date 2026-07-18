package beer.thierry.centsible.core.services.loans

import beer.thierry.centsible.api.exceptions.LocalizedException
import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.contact.ContactForm
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.loan.LoanDTO
import beer.thierry.centsible.api.model.loan.LoanForm
import beer.thierry.centsible.api.model.loan.LoanUpdateForm
import beer.thierry.centsible.api.model.loan.OutstandingTotalDTO
import beer.thierry.centsible.api.model.loan.RepaymentDTO
import beer.thierry.centsible.api.model.loan.RepaymentForm
import beer.thierry.centsible.api.model.loan.SplitToLoansForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.IBudgetAccountsRepository
import beer.thierry.centsible.api.repository.IContactsRepository
import beer.thierry.centsible.api.repository.ILoanRepaymentsRepository
import beer.thierry.centsible.api.repository.ILoansRepository
import beer.thierry.centsible.api.repository.ITransactionRepository
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
    private val transactionRepository: ITransactionRepository,
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

        val loanCurrency: Currency = form.currency
            ?: form.accountId?.takeIf { form.affectBalance }?.let { budgetAccountsRepository.fetchAccountCurrency(it) }
            ?: parseCurrency(authenticatedUser.defaultCurrency)

        form.interestRate?.let { rate -> form.owedAmount = applyInterest(form.lentAmount, rate) }

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

    override fun totalOutstanding(authenticatedUser: UserDTO): OutstandingTotalDTO {
        val defaultCurrency = parseCurrency(
            userRepository.findUserById(authenticatedUser.id)?.defaultCurrency ?: authenticatedUser.defaultCurrency
        )
        val today = LocalDate.now()
        var excluded = 0
        val total = loansRepository.fetchAllLoans(authenticatedUser)
            .filter { it.outstanding.signum() > 0 }
            .fold(BigDecimal.ZERO) { acc, loan ->
                val converted = runCatching {
                    currencyConversionService
                        .convert(loan.outstanding, parseCurrency(loan.currency), defaultCurrency, today)
                        .convertedAmount
                }.getOrElse {
                    excluded++
                    log.warn("Excluding loan {} ({}) from outstanding total: FX unavailable", loan.id, loan.currency, it)
                    BigDecimal.ZERO
                }
                acc + converted
            }
            .setScale(2, RoundingMode.HALF_UP)
        return OutstandingTotalDTO(outstanding = total, excludedCount = excluded)
    }

    /** owed = lent * (1 + rate/100), rounded to 2dp. */
    private fun applyInterest(lent: BigDecimal, ratePercent: BigDecimal): BigDecimal =
        lent.multiply(BigDecimal.ONE.add(ratePercent.movePointLeft(2))).setScale(2, RoundingMode.HALF_UP)

    private fun parseCurrency(code: String?): Currency = Currency.parseOrNull(code) ?: Currency.EUR

    private fun resolveContactId(authenticatedUser: UserDTO, form: LoanForm): UUID =
        resolveContact(authenticatedUser, form.contactId, form.newContactFirstName, form.newContactLastName)

    /** Resolves an existing contact (validating ownership) or creates a new one from the given names. */
    private fun resolveContact(
        authenticatedUser: UserDTO,
        existing: UUID?,
        newFirstNameRaw: String?,
        newLastNameRaw: String?,
    ): UUID {
        val newFirstName = newFirstNameRaw?.takeIf { it.isNotBlank() }

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
            ContactForm(firstName = newFirstName, lastName = newLastNameRaw?.takeIf { it.isNotBlank() })
        )
        return created.id ?: throw IllegalStateException("Failed to create contact.")
    }

    @Transactional
    override fun splitTransactionIntoLoans(
        authenticatedUser: UserDTO,
        transactionId: UUID,
        form: SplitToLoansForm,
    ): List<LoanDTO> {
        val transaction = transactionRepository.fetchTransactionById(transactionId, authenticatedUser)
        if (transaction.type != CategoryType.EXPENSE) {
            throw LocalizedException.BadRequest("error.loan.split.notExpense")
        }
        if (transaction.transferGroupId != null) {
            throw LocalizedException.BadRequest("error.loan.split.transfer")
        }

        val shares = form.shares
        if (shares.isEmpty()) throw LocalizedException.BadRequest("error.loan.split.empty")

        val total = shares.fold(BigDecimal.ZERO) { acc, share -> acc.add(share.amount) }
        val transactionAmount = transaction.amount ?: BigDecimal.ZERO
        if (total.signum() <= 0 || total > transactionAmount) {
            throw LocalizedException.BadRequest("error.loan.split.amountRange")
        }

        val currency = form.currency ?: parseCurrency(authenticatedUser.defaultCurrency)
        val description = form.description?.takeIf { it.isNotBlank() }
            ?: transaction.description?.takeIf { it.isNotBlank() }
            ?: "Split"
        val today = LocalDate.now()

        val created = shares.map { share ->
            val contactId = resolveContact(
                authenticatedUser, share.contactId, share.newContactFirstName, share.newContactLastName,
            )
            loansRepository.createIouLoan(
                authenticatedUser,
                contactId = contactId,
                sourceTransactionId = transactionId,
                amount = share.amount,
                currency = currency,
                description = description,
                loanDate = today,
                dueDate = share.dueDate,
                note = share.note?.takeIf { it.isNotBlank() },
            )
        }
        log.info(
            "Split transaction id={} into {} IOUs userId={} total={}",
            transactionId, created.size, authenticatedUser.id, total,
        )
        return created
    }
}
