package beer.thierry.centsible.core.service

import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryForm
import beer.thierry.centsible.api.model.category.CategoryType
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.repository.ICategoriesRepository
import beer.thierry.centsible.core.services.categories.CategoryService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class CategoryServiceTest {

    @Mock
    private lateinit var repository: ICategoriesRepository

    @InjectMocks
    private lateinit var service: CategoryService

    private val user = UserDTO(UUID.randomUUID(), "u", "u@x", "U", "X", "U X", null)

    @Test
    fun `fetchAllCategories delegates to repository`() {
        val categories = listOf(CategoryDTO(1L, "Food", "icon", "#fff", CategoryType.EXPENSE))
        `when`(repository.fetchAllCategories(user)).thenReturn(categories)

        assertEquals(categories, service.fetchAllCategories(user))
    }

    @Test
    fun `deleteCategory refuses when category is still referenced`() {
        `when`(repository.isCategoryUsed(user, 5L)).thenReturn(true)

        val ex = assertThrows(IllegalArgumentException::class.java) {
            service.deleteCategory(user, 5L)
        }
        assertTrue(ex.message!!.contains("referenced"))
        verify(repository, never()).deleteCategory(user, 5L)
    }

    @Test
    fun `deleteCategory delegates when category is unused`() {
        `when`(repository.isCategoryUsed(user, 5L)).thenReturn(false)
        `when`(repository.deleteCategory(user, 5L)).thenReturn(true)

        assertEquals(true, service.deleteCategory(user, 5L))
        verify(repository).deleteCategory(user, 5L)
    }

    @Test
    fun `createCategory delegates to repository`() {
        val form = CategoryForm("Food", "icon", "#fff", CategoryType.EXPENSE)
        val created = CategoryDTO(1L, "Food", "icon", "#fff", CategoryType.EXPENSE)
        `when`(repository.createCategory(user, form)).thenReturn(created)

        assertEquals(created, service.createCategory(user, form))
    }
}
