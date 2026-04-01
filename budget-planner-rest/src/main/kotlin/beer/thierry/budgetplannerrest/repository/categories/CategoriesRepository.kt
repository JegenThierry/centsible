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
        return dsl.select(CATEGORIES.ID, CATEGORIES.NAME, CATEGORIES.ICON)
            .from(CATEGORIES)
            .where(CATEGORIES.USER_ID.eq(authenticatedUser.id).or(CATEGORIES.USER_ID.isNull))
            .fetch { record ->
                CategoryDTO(
                    id = record[CATEGORIES.ID],
                    name = record[CATEGORIES.NAME],
                    icon = record[CATEGORIES.ICON]
                )
            }
    }

    override fun fetchCategoryById(authenticatedUser: UserDTO, id: Long): CategoryDTO? {
        return dsl.select(CATEGORIES.ID, CATEGORIES.NAME, CATEGORIES.ICON)
            .from(CATEGORIES)
            .where((CATEGORIES.USER_ID.eq(authenticatedUser.id).or(CATEGORIES.USER_ID.isNull)).and(CATEGORIES.ID.eq(id)))
            .fetchOne { record ->
                CategoryDTO(
                    id = record[CATEGORIES.ID],
                    name = record[CATEGORIES.NAME],
                    icon = record[CATEGORIES.ICON]
                )
            }
    }

    override fun createCategory(authenticatedUser: UserDTO, category: CategoryForm): CategoryDTO {
        val record = dsl.insertInto(CATEGORIES)
            .set(CATEGORIES.NAME, category.name)
            .set(CATEGORIES.ICON, category.icon)
            .set(CATEGORIES.USER_ID, authenticatedUser.id)
            .set(CATEGORIES.CREATED_AT, OffsetDateTime.now())
            .returning(CATEGORIES.ID, CATEGORIES.NAME, CATEGORIES.ICON)
            .fetchOne() ?: throw IllegalStateException("Failed to retrieve generated Category")

        return CategoryDTO(
            id = record[CATEGORIES.ID],
            name = record[CATEGORIES.NAME],
            icon = record[CATEGORIES.ICON]
        )
    }

    override fun updateCategory(authenticatedUser: UserDTO, id: Long, category: CategoryForm): CategoryDTO? {
        val record = dsl.update(CATEGORIES)
            .set(CATEGORIES.NAME, category.name)
            .set(CATEGORIES.ICON, category.icon)
            .where(CATEGORIES.USER_ID.eq(authenticatedUser.id).and(CATEGORIES.ID.eq(id)))
            .returning(CATEGORIES.ID, CATEGORIES.NAME, CATEGORIES.ICON)
            .fetchOne()

        return record?.let {
            CategoryDTO(
                id = it[CATEGORIES.ID],
                name = it[CATEGORIES.NAME],
                icon = it[CATEGORIES.ICON]
            )
        }
    }

    override fun deleteCategory(authenticatedUser: UserDTO, id: Long): Boolean {
        return dsl.deleteFrom(CATEGORIES)
            .where(CATEGORIES.USER_ID.eq(authenticatedUser.id).and(CATEGORIES.ID.eq(id)))
            .execute() > 0
    }
}
