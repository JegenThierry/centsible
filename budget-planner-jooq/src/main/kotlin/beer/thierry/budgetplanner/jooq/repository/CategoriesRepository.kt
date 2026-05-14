package beer.thierry.budgetplanner.jooq.repository

import beer.thierry.budgetplanner.api.model.category.CategoryDTO
import beer.thierry.budgetplanner.api.model.category.CategoryForm
import beer.thierry.budgetplanner.api.model.category.CategoryType
import beer.thierry.budgetplanner.api.model.user.UserDTO
import beer.thierry.budgetplanner.api.repository.ICategoriesRepository
import beer.thierry.jooq.generated.tables.references.CATEGORIES
import beer.thierry.jooq.generated.tables.references.TRANSACTIONS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class CategoriesRepository(private val dsl: DSLContext) : ICategoriesRepository {
    override fun fetchAllCategories(authenticatedUser: UserDTO): List<CategoryDTO> {
        return dsl.select(
            CATEGORIES.ID,
            CATEGORIES.NAME,
            CATEGORIES.ICON,
            CATEGORIES.COLOR,
            CATEGORIES.TYPE,
            CATEGORIES.USER_ID
        )
            .from(CATEGORIES)
            .where(CATEGORIES.USER_ID.eq(authenticatedUser.id).or(CATEGORIES.USER_ID.isNull))
            .and(CATEGORIES.IS_MANAGED.isFalse)
            .fetch { record ->
                CategoryDTO(
                    id = record[CATEGORIES.ID],
                    name = record[CATEGORIES.NAME],
                    icon = record[CATEGORIES.ICON],
                    color = record[CATEGORIES.COLOR],
                    type = CategoryType.fromValue(record[CATEGORIES.TYPE]!!),
                    isSystem = record[CATEGORIES.USER_ID] == null
                )
            }
    }

    override fun fetchCategoryById(authenticatedUser: UserDTO, id: Long): CategoryDTO? {
        return dsl.select(
            CATEGORIES.ID,
            CATEGORIES.NAME,
            CATEGORIES.ICON,
            CATEGORIES.COLOR,
            CATEGORIES.TYPE,
            CATEGORIES.USER_ID
        )
            .from(CATEGORIES)
            .where(
                (CATEGORIES.USER_ID.eq(authenticatedUser.id).or(CATEGORIES.USER_ID.isNull))
                    .and(CATEGORIES.ID.eq(id))
                    .and(CATEGORIES.IS_MANAGED.isFalse)
            )
            .fetchOne { record ->
                CategoryDTO(
                    id = record[CATEGORIES.ID],
                    name = record[CATEGORIES.NAME],
                    icon = record[CATEGORIES.ICON],
                    color = record[CATEGORIES.COLOR],
                    type = CategoryType.fromValue(record[CATEGORIES.TYPE]!!),
                    isSystem = record[CATEGORIES.USER_ID] == null
                )
            }
    }

    override fun createCategory(authenticatedUser: UserDTO, category: CategoryForm): CategoryDTO {
        val record = dsl.insertInto(CATEGORIES)
            .set(CATEGORIES.NAME, category.name)
            .set(CATEGORIES.ICON, category.icon)
            .set(CATEGORIES.COLOR, category.color)
            .set(CATEGORIES.TYPE, category.type.value)
            .set(CATEGORIES.USER_ID, authenticatedUser.id)
            .set(CATEGORIES.CREATED_AT, OffsetDateTime.now())
            .returning(
                CATEGORIES.ID,
                CATEGORIES.NAME,
                CATEGORIES.ICON,
                CATEGORIES.COLOR,
                CATEGORIES.TYPE,
                CATEGORIES.USER_ID
            )
            .fetchOne() ?: throw IllegalStateException("Failed to retrieve generated Category")

        return CategoryDTO(
            id = record[CATEGORIES.ID],
            name = record[CATEGORIES.NAME],
            icon = record[CATEGORIES.ICON],
            color = record[CATEGORIES.COLOR],
            type = CategoryType.fromValue(record[CATEGORIES.TYPE]!!),
            isSystem = record[CATEGORIES.USER_ID] == null
        )
    }

    override fun updateCategory(authenticatedUser: UserDTO, id: Long, category: CategoryForm): CategoryDTO? {
        val record = dsl.update(CATEGORIES)
            .set(CATEGORIES.NAME, category.name)
            .set(CATEGORIES.ICON, category.icon)
            .set(CATEGORIES.COLOR, category.color)
            .set(CATEGORIES.TYPE, category.type.value)
            .where(CATEGORIES.USER_ID.eq(authenticatedUser.id).and(CATEGORIES.ID.eq(id)))
            .returning(
                CATEGORIES.ID,
                CATEGORIES.NAME,
                CATEGORIES.ICON,
                CATEGORIES.COLOR,
                CATEGORIES.TYPE,
                CATEGORIES.USER_ID
            )
            .fetchOne()

        return record?.let {
            CategoryDTO(
                id = it[CATEGORIES.ID],
                name = it[CATEGORIES.NAME],
                icon = it[CATEGORIES.ICON],
                color = it[CATEGORIES.COLOR],
                type = CategoryType.fromValue(it[CATEGORIES.TYPE]!!),
                isSystem = it[CATEGORIES.USER_ID] == null
            )
        }
    }

    override fun deleteCategory(authenticatedUser: UserDTO, id: Long): Boolean {
        return dsl.deleteFrom(CATEGORIES)
            .where(CATEGORIES.USER_ID.eq(authenticatedUser.id).and(CATEGORIES.ID.eq(id)))
            .execute() > 0
    }

    override fun isCategoryUsed(id: Long): Boolean {
        return dsl.fetchExists(
            dsl.selectOne()
                .from(TRANSACTIONS)
                .where(TRANSACTIONS.CATEGORY_ID.eq(id))
        )
    }
}
