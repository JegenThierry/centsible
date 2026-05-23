package beer.thierry.centsible.core.services.integrations

import beer.thierry.centsible.api.model.user.UserDTO
import java.util.UUID

/**
 * Builds a UserDTO carrying only the id. Several integrations services need to call
 * user-scoped repositories from contexts where there is no authenticated principal — the
 * scheduled sync worker, the public OAuth callback, and the token-refresh guard. The
 * repository layer only reads `id` (queries are `WHERE user_id = :id`), so the other DTO
 * fields can be defaulted. Defined once here to avoid drifting copies across the package.
 */
internal fun syntheticUser(id: UUID): UserDTO = UserDTO().apply { this.id = id }

/**
 * Returns a copy of this map with `patch`'s entries put on top, preserving insertion order and
 * leaving the receiver untouched. Used wherever a stored config map is updated by merging a
 * provider's configPatch into the row's current config.
 */
internal fun <K, V> Map<K, V>.mergedWith(patch: Map<K, V>): MutableMap<K, V> =
    toMutableMap().apply { putAll(patch) }
