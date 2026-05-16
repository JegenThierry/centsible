package beer.thierry.centsible.api.services.integrations

import beer.thierry.centsible.api.model.integrations.ProviderDescriptor

interface IProviderRegistry {
    fun listDescriptors(): List<ProviderDescriptor>
    fun getDescriptor(key: String): ProviderDescriptor?
    fun getModule(key: String): ProviderModule?
}
