package beer.thierry.centsible.integrations.manual

import beer.thierry.centsible.api.model.integrations.AuthType
import beer.thierry.centsible.api.model.integrations.ConfigField
import beer.thierry.centsible.api.model.integrations.FieldType
import beer.thierry.centsible.api.model.integrations.ProviderDescriptor
import beer.thierry.centsible.api.services.integrations.ProviderModule

class ManualProviderModule : ProviderModule {
    override val descriptor = ProviderDescriptor(
        key = "manual",
        displayName = "Manual entry",
        description = "Track an external account by hand. Useful for cash, prepaid cards, or anything " +
            "without an API.",
        authType = AuthType.NONE,
        capabilities = emptySet(),
        configFields = listOf(
            ConfigField(
                name = "label",
                label = "Account label",
                type = FieldType.STRING,
                required = true,
                placeholder = "e.g. Wallet, Travel card, Vending stash",
            ),
            ConfigField(
                name = "notes",
                label = "Notes",
                type = FieldType.MULTILINE,
                required = false,
                helpText = "Optional. Free-form notes about this manual account.",
            ),
        ),
    )
}
