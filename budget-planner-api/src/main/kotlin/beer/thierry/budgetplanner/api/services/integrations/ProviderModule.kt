package beer.thierry.budgetplanner.api.services.integrations

import beer.thierry.budgetplanner.api.model.integrations.ExternalAccountDTO
import beer.thierry.budgetplanner.api.model.integrations.ProviderContext
import beer.thierry.budgetplanner.api.model.integrations.ProviderDescriptor
import beer.thierry.budgetplanner.api.model.integrations.QuoteDTO
import beer.thierry.budgetplanner.api.model.integrations.TransactionImportPage

/**
 * Base contract every integration provider implements. Capability interfaces below are mixins —
 * a provider only implements those that apply. Provider modules depend only on
 * budget-planner-api and are auto-discovered via Spring component scanning.
 */
interface ProviderModule {
    val descriptor: ProviderDescriptor

    /**
     * Lightweight reachability check executed when a user creates or edits a connection.
     * Should not perform a full sync. Default: no-op (provider accepts any config).
     */
    fun testConnection(ctx: ProviderContext) {}
}

interface IAccountProvider {
    fun listExternalAccounts(ctx: ProviderContext): List<ExternalAccountDTO>
}

interface ITransactionImporter {
    fun importSince(ctx: ProviderContext, cursor: String?): TransactionImportPage
}

interface IQuoteProvider {
    fun fetchQuotes(ctx: ProviderContext, symbols: List<String>): List<QuoteDTO>
}
