package beer.thierry.centsible.api.model.integrations

enum class AuthType { OAUTH2, API_KEY, BASIC, NONE }

enum class Capability { ACCOUNTS, TRANSACTIONS, QUOTES, OAUTH_FLOW }

/**
 * Field rendered by the UI's dynamic config form.
 *
 * SELECT_REMOTE → options are fetched lazily from the backend
 * (POST /api/integrations/providers/{key}/options/{field} with the current form values),
 * so the provider can return institution lists, account pickers, etc. without baking
 * thousands of static options into the descriptor.
 *
 * OAUTH_LAUNCH → renders a "Continue to {provider}" button instead of an input. Clicked,
 * the UI POSTs /connections/{id}/oauth/start and follows the returned authorizationUrl.
 * Used by providers with authType=OAUTH2.
 */
enum class FieldType { STRING, NUMBER, BOOLEAN, SELECT, MULTILINE, SELECT_REMOTE, OAUTH_LAUNCH }

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
    /**
     * For SELECT_REMOTE: names of other fields whose values should be sent when fetching
     * options. Allows dependent dropdowns (e.g. institutionId filtered by country).
     */
    val dependsOn: List<String> = emptyList(),
)

data class ProviderDescriptor(
    val key: String,
    val displayName: String,
    val description: String? = null,
    val authType: AuthType,
    val capabilities: Set<Capability> = emptySet(),
    val configFields: List<ConfigField> = emptyList(),
)
