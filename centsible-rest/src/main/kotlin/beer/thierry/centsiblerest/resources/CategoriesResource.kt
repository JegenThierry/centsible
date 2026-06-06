package beer.thierry.centsiblerest.resources

import beer.thierry.centsible.api.model.category.CategoryDTO
import beer.thierry.centsible.api.model.category.CategoryForm
import beer.thierry.centsible.api.model.user.UserDTO
import beer.thierry.centsible.api.services.categories.ICategoryService
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RequestMapping("/api/categories")
@RestController
class CategoriesResource(private val categoryService: ICategoryService) {

    private val log = LoggerFactory.getLogger(CategoriesResource::class.java)

    @PostMapping
    fun create(
        @Valid @RequestBody category: CategoryForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<CategoryDTO> {
        val created = categoryService.createCategory(authenticatedUser, category)
        log.info("Created category id={} userId={}", created.id, authenticatedUser.id)
        return ResponseEntity.ok(created)
    }

    @GetMapping
    fun getCategories(@AuthenticationPrincipal authenticatedUser: UserDTO): ResponseEntity<List<CategoryDTO>> =
        ResponseEntity.ok(categoryService.fetchAllCategories(authenticatedUser))

    @GetMapping("/{id}")
    fun getCategory(
        @PathVariable id: Long,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<CategoryDTO> {
        val result = categoryService.fetchCategoryById(authenticatedUser, id)
            ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(result)
    }

    @PutMapping("/{id}")
    fun updateCategory(
        @PathVariable id: Long,
        @Valid @RequestBody category: CategoryForm,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<CategoryDTO> {
        val result = categoryService.updateCategory(authenticatedUser, id, category)
            ?: return ResponseEntity.notFound().build()
        log.info("Updated category id={} userId={}", id, authenticatedUser.id)
        return ResponseEntity.ok(result)
    }

    @DeleteMapping("/{id}")
    fun deleteCategory(
        @PathVariable id: Long,
        @AuthenticationPrincipal authenticatedUser: UserDTO,
    ): ResponseEntity<Void> {
        val deleted = categoryService.deleteCategory(authenticatedUser, id)
        return if (deleted) {
            log.info("Deleted category id={} userId={}", id, authenticatedUser.id)
            ResponseEntity.ok().build()
        } else ResponseEntity.notFound().build()
    }
}
