package beer.thierry.centsible.api.repository

import beer.thierry.centsible.api.model.export.ExportAccountRow
import beer.thierry.centsible.api.model.export.ExportContactSummaryRow
import beer.thierry.centsible.api.model.export.ExportLoanRow
import beer.thierry.centsible.api.model.export.ExportTransactionRow
import beer.thierry.centsible.api.model.user.UserDTO
import java.time.LocalDate
import java.util.UUID

interface IExportDataRepository {

    fun fetchUserById(userId: UUID): UserDTO?

    fun fetchTransactionsForExport(
        userId: UUID,
        accountIds: List<UUID>,
        fromDate: LocalDate?,
        toDate: LocalDate?,
        categoryIds: List<Long>,
    ): List<ExportTransactionRow>

    fun fetchAccountsByIds(userId: UUID, accountIds: List<UUID>): List<ExportAccountRow>

    fun fetchAllAccounts(userId: UUID, asOfDate: LocalDate?): List<ExportAccountRow>

    fun fetchLoansForContact(userId: UUID, contactId: UUID): List<ExportLoanRow>

    fun fetchAllLoans(userId: UUID, includeSettled: Boolean): List<ExportLoanRow>

    fun fetchContactSummary(userId: UUID, contactId: UUID): ExportContactSummaryRow?

    fun fetchAllContactSummaries(userId: UUID): List<ExportContactSummaryRow>
}
