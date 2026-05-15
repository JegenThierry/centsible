package beer.thierry.budgetplanner.api.services.integrations

import beer.thierry.budgetplanner.api.model.integrations.ProviderDescriptor

interface IProviderRegistry {
    fun listDescriptors(): List<ProviderDescriptor>
    fun getDescriptor(key: String): ProviderDescriptor?
    fun getModule(key: String): ProviderModule?
}
