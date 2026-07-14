package beer.thierry.centsible.api.services.loans

import beer.thierry.centsible.api.model.loan.LoanDTO
import beer.thierry.centsible.api.model.loan.LoanForm
import beer.thierry.centsible.api.model.loan.LoanUpdateForm
import beer.thierry.centsible.api.model.loan.OutstandingTotalDTO
import beer.thierry.centsible.api.model.loan.RepaymentDTO
import beer.thierry.centsible.api.model.loan.RepaymentForm
import beer.thierry.centsible.api.model.loan.SplitToLoansForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

interface ILoanService {
    fun fetchAllLoans(authenticatedUser: UserDTO): List<LoanDTO>
    fun fetchLoansByContact(authenticatedUser: UserDTO, contactId: UUID): List<LoanDTO>
    fun fetchLoanById(authenticatedUser: UserDTO, id: UUID): LoanDTO?
    fun createLoan(authenticatedUser: UserDTO, form: LoanForm): LoanDTO

    /**
     * Splits an existing expense [transactionId] into tracking-only IOUs, one loan per share. The
     * shares must sum to at most the transaction amount; the remainder stays the user's own expense.
     */
    fun splitTransactionIntoLoans(authenticatedUser: UserDTO, transactionId: UUID, form: SplitToLoansForm): List<LoanDTO>
    fun updateLoan(authenticatedUser: UserDTO, id: UUID, form: LoanUpdateForm): LoanDTO?
    fun deleteLoan(authenticatedUser: UserDTO, id: UUID): Boolean

    fun fetchRepayments(authenticatedUser: UserDTO, loanId: UUID): List<RepaymentDTO>
    fun recordRepayment(authenticatedUser: UserDTO, loanId: UUID, form: RepaymentForm): RepaymentDTO
    fun deleteRepayment(authenticatedUser: UserDTO, loanId: UUID, repaymentId: UUID): Boolean

    /** Aggregates every open loan's remaining balance into the user's default currency. */
    fun totalOutstanding(authenticatedUser: UserDTO): OutstandingTotalDTO
}
