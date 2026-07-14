package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.budgetaccount.Currency
import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.loan.LoanDTO
import beer.thierry.centsible.api.model.loan.LoanForm
import beer.thierry.centsible.api.model.loan.LoanUpdateForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

interface ILoansRepository {
    fun fetchAllLoans(authenticatedUser: UserDTO): List<LoanDTO>
    fun fetchLoansByContact(authenticatedUser: UserDTO, contactId: UUID): List<LoanDTO>
    fun fetchLoanById(authenticatedUser: UserDTO, id: UUID): LoanDTO?

    /**
     * @param currency the resolved loan currency (already defaulted by the service).
     * @param conversion FX result for the lending transaction when the loan affects a balance; null
     *   for tracking-only loans. The lending transaction is stored in the account currency
     *   (`conversion.convertedAmount`) while the loan keeps its own [currency].
     */
    fun createLoan(
        authenticatedUser: UserDTO,
        contactId: UUID,
        form: LoanForm,
        currency: Currency,
        conversion: ConversionResult?,
    ): LoanDTO

    /**
     * Creates a tracking-only IOU loan (no lending transaction, does not touch any balance) linked
     * back to [sourceTransactionId], the expense it was carved from. `lent` and `owed` both equal
     * [amount] (IOUs carry no interest).
     */
    fun createIouLoan(
        authenticatedUser: UserDTO,
        contactId: UUID,
        sourceTransactionId: UUID,
        amount: BigDecimal,
        currency: Currency,
        description: String,
        loanDate: LocalDate,
        dueDate: LocalDate?,
        note: String?,
    ): LoanDTO

    /** Balance-neutral update; returns null when the loan does not exist or is not owned by the user. */
    fun updateLoan(authenticatedUser: UserDTO, id: UUID, form: LoanUpdateForm): LoanDTO?

    fun deleteLoan(authenticatedUser: UserDTO, id: UUID): Boolean
}
