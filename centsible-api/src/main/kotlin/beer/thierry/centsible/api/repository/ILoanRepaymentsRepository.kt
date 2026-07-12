package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.currency.ConversionResult
import beer.thierry.centsible.api.model.loan.RepaymentDTO
import beer.thierry.centsible.api.model.loan.RepaymentForm
import beer.thierry.centsible.api.model.user.UserDTO
import java.math.BigDecimal
import java.util.UUID

interface ILoanRepaymentsRepository {
    fun fetchRepayments(authenticatedUser: UserDTO, loanId: UUID): List<RepaymentDTO>

    /**
     * @param conversion FX result for the repayment transaction when it affects a balance (converts the
     *   loan-currency [RepaymentForm.amount] into the repayment account currency); null for
     *   tracking-only repayments. The stored repayment amount stays in the loan currency.
     */
    fun createRepayment(
        authenticatedUser: UserDTO,
        loanId: UUID,
        form: RepaymentForm,
        conversion: ConversionResult?,
    ): RepaymentDTO

    fun deleteRepayment(authenticatedUser: UserDTO, loanId: UUID, repaymentId: UUID): Boolean
    fun totalRepaidForLoan(loanId: UUID): BigDecimal
}
