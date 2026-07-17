package beer.thierry.centsible.core.services.integrations

import beer.thierry.centsible.api.model.integrations.ProviderDescriptor
import beer.thierry.centsible.api.services.integrations.IProviderRegistry
import beer.thierry.centsible.api.services.integrations.ProviderModule
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ProviderRegistry(modules: List<ProviderModule>) : IProviderRegistry {

    private val byKey: Map<String, ProviderModule>

    init {
        val grouped = modules.groupBy { it.descriptor.key }
        val duplicates = grouped.filter { it.value.size > 1 }.keys
        require(duplicates.isEmpty()) {
            "Duplicate integration provider keys: $duplicates. Each ProviderModule.descriptor.key must be unique."
        }
        byKey = grouped.mapValues { it.value.single() }
        val log = LoggerFactory.getLogger(ProviderRegistry::class.java)
        log.info(
            "Registered {} integration provider(s): {}",
            byKey.size,
            byKey.keys.sorted().joinToString(", ").ifEmpty { "<none>" },
        )
    }

    override fun listDescriptors(): List<ProviderDescriptor> =
        byKey.values.map { it.descriptor }.sortedBy { it.displayName }

    override fun getDescriptor(key: String): ProviderDescriptor? = byKey[key]?.descriptor

    override fun getModule(key: String): ProviderModule? = byKey[key]
}
