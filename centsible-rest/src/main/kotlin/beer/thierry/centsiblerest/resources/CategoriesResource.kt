package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.categories.ICategoryService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/categories")
@RestController
class CategoriesResource(private val categoryService: ICategoryService) {

    @PostMapping
    fun create(
        @Valid @RequestBody category: CategoryForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO?
    ): ResponseEntity<CategoryDTO> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build()
        }
        val result = categoryService.createCategory(authenticatedUser, category)
        return ResponseEntity.ok(result)
    }

    @GetMapping
    fun getCategories(
        @AuthenticationPrincipal authenticatedUser: UserDTO?
    ): ResponseEntity<List<CategoryDTO>> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build()
        }
        val result = categoryService.fetchAllCategories(authenticatedUser)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{id}")
    fun getCategory(
        @PathVariable id: Long,
        @AuthenticationPrincipal authenticatedUser: UserDTO?
    ): ResponseEntity<CategoryDTO> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build()
        }
        val result = categoryService.fetchCategoryById(authenticatedUser, id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }

    @PutMapping("/{id}")
    fun updateCategory(
        @PathVariable id: Long,
        @Valid @RequestBody category: CategoryForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO?
    ): ResponseEntity<CategoryDTO> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build()
        }
        val result = categoryService.updateCategory(authenticatedUser, id, category)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }

    @DeleteMapping("/{id}")
    fun deleteCategory(
        @PathVariable id: Long,
        @AuthenticationPrincipal authenticatedUser: UserDTO?
    ): ResponseEntity<Void> {
        if (authenticatedUser == null) {
            return ResponseEntity.status(401).build()
        }
        val deleted = categoryService.deleteCategory(authenticatedUser, id)
        return if (deleted) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
