package beer.thierry.centsible.api.services.integrations

import beer.thierry.centsible.api.model.integrations.ProviderDescriptor

/**
 * Lookup over Spring-discovered provider SPIs by key. A [ProviderDescriptor] is the static
 * metadata (capabilities, config fields); a [ProviderModule] is the live SPI instance.
 */
interface IProviderRegistry {
    fun listDescriptors(): List<ProviderDescriptor>
    fun getDescriptor(key: String): ProviderDescriptor?
    fun getModule(key: String): ProviderModule?
}
