package beer.thierry.centsible.core.services.integrations

import beer.thierry.centsible.api.model.integrations.ProviderDescriptor
import beer.thierry.centsible.api.services.integrations.ProviderModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ProviderRegistryTest {

    private fun module(key: String, minInterval: Long): ProviderModule {
        val descriptor = mock(ProviderDescriptor::class.java)
        `when`(descriptor.key).thenReturn(key)
        val module = mock(ProviderModule::class.java)
        `when`(module.descriptor).thenReturn(descriptor)
        `when`(module.minSyncIntervalSeconds).thenReturn(minInterval)
        return module
    }

    @Test
    fun `syncIntervalsByKey returns only providers that declare a floor`() {
        val registry = ProviderRegistry(
            listOf(module("banking-gocardless", 21600L), module("paypal", 0L)),
        )

        assertEquals(mapOf("banking-gocardless" to 21600L), registry.syncIntervalsByKey())
    }
}
