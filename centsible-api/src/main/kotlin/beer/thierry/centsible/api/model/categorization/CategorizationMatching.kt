package beer.thierry.centsible.api.model.categorization

import beer.thierry.centsible.api.model.category.CategoryType

fun CategorizationRuleDTO.matches(description: String): Boolean = when (matchType) {
    MatchType.CONTAINS -> description.contains(pattern, ignoreCase = true)
    MatchType.EQUALS -> description.equals(pattern, ignoreCase = true)
    MatchType.STARTS_WITH -> description.startsWith(pattern, ignoreCase = true)
}

fun List<CategorizationRuleDTO>.firstMatch(description: String, type: CategoryType): CategorizationRuleDTO? =
    firstOrNull { it.category.type == type && it.matches(description) }
