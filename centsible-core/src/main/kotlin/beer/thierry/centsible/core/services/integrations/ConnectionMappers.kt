package beer.thierry.centsible.core.services.integrations

import beer.thierry.centsible.api.model.integrations.ProviderConnectionDTO
import beer.thierry.centsible.api.repository.ProviderConnectionRecord
import io.mcarle.konvert.api.Konverter

/**
 * Konvert generates a `ConnectionMappersImpl` object at build time with the field-by-field copy
 * from [ProviderConnectionRecord] to [ProviderConnectionDTO]. The two types are intentionally
 * parallel (domain record vs transport DTO) so a generated mapping is a stronger guarantee than
 * hand-rolled copy code — a rename or added field breaks the build instead of leaking through.
 */
@Konverter
interface ConnectionMappers {
    fun toDTO(record: ProviderConnectionRecord): ProviderConnectionDTO
}
