package beer.thierry.budgetplannerrest.repository.categories

import beer.thierry.budgetplannerrest.model.category.CategoryDTO
import beer.thierry.budgetplannerrest.model.category.CategoryForm
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class CategoriesRepository(private val dsl: DSLContext) : ICategoriesRepository {
    override fun fetchAllCategories(authenticatedUser: UserDTO): List<CategoryDTO> {
        return dsl.select(CATEGORIES.ID, CATEGORIES.NAME)
            .from(CATEGORIES)
            .where(CATEGORIES.USER_ID.eq(authenticatedUser.id).or(CATEGORIES.USER_ID.isNull))
            .fetchInto(CategoryDTO::class.java)
    }

    override fun fetchCategoryById(authenticatedUser: UserDTO): List<CategoryDTO> {
        return dsl.select(CATEGORIES.ID, CATEGORIES.NAME)
            .from(CATEGORIES)
            .where(
                (CATEGORIES.USER_ID.eq(authenticatedUser.id).or(CATEGORIES.USER_ID.isNull)).and(
                    CATEGORIES.ID.eq(
                        CATEGORIES.ID
                    )
                )
            )
            .fetchInto(CategoryDTO::class.java)
    }

    override fun createCategory(
        authenticatedUser: UserDTO,
        category: CategoryForm
    ): CategoryDTO {
        return dsl.insertInto(CATEGORIES)
            .set(CATEGORIES.NAME, category.name)
            .set(CATEGORIES.TYPE, category.type.toString())
            .set(CATEGORIES.ICON, category.icon.toString())
            .set(CATEGORIES.USER_ID, authenticatedUser.id)
            .set(CATEGORIES.CREATED_AT, OffsetDateTime.now())
            .returning(CATEGORIES.ID, CATEGORIES.NAME, CATEGORIES.TYPE, CATEGORIES.ICON, CATEGORIES.CREATED_AT)
            .fetchOneInto(CategoryDTO::class.java)
            ?: throw IllegalStateException("Failed to retrieve generated Account")
    }
}
