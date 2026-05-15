package beer.thierry.budgetplanner.api.model.integrations

enum class AuthType { OAUTH2, API_KEY, BASIC, NONE }

enum class Capability { ACCOUNTS, TRANSACTIONS, QUOTES }

enum class FieldType { STRING, NUMBER, BOOLEAN, SELECT, MULTILINE }

data class SelectOption(
    val value: String,
    val label: String,
)

data class ConfigField(
    val name: String,
    val label: String,
    val type: FieldType,
    val required: Boolean = false,
    val secret: Boolean = false,
    val options: List<SelectOption> = emptyList(),
    val placeholder: String? = null,
    val helpText: String? = null,
)

data class ProviderDescriptor(
    val key: String,
    val displayName: String,
    val description: String? = null,
    val authType: AuthType,
    val capabilities: Set<Capability> = emptySet(),
    val configFields: List<ConfigField> = emptyList(),
)
