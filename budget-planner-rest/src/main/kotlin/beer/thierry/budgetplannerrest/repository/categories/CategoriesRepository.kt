package beer.thierry.budgetplannerrest.repository.categories

import beer.thierry.budgetplannerrest.model.category.CategoryDTO
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class CategoriesRepository(private val dsl: DSLContext) : ICategoriesRepository {
    override fun fetchAllCategories(): List<CategoryDTO> {
        return dsl.select(CATEGORIES.NAME)
            .from(CATEGORIES)
            .fetchInto(CategoryDTO::class.java)
    }
}