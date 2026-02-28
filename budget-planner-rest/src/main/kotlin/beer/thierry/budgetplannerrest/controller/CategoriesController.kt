package beer.thierry.budgetplannerrest.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/categories")
@RestController
class CategoriesController {
//    @PostMapping
//    fun create(@RequestBody category: CategoryForm): ResponseEntity<CategoryDTO> {
//        return ResponseEntity.ok()
//    }
//
    @GetMapping
    fun getCategories(): ResponseEntity<String> {
        return ResponseEntity.ok("hello you are authorized")
    }
//
//    @GetMapping("{id}")
//    fun getCategory(@PathVariable id: Long): ResponseEntity<CategoryDTO> {
//        return ResponseEntity.ok()
//    }
//
//    @PutMapping("{id}")
//    fun updateCategory(@PathVariable id: Long, @RequestBody category: CategoryForm): ResponseEntity<CategoryDTO> {
//        return ResponseEntity.ok()
//    }
//
//    @DeleteMapping("{id}")
//    fun deleteCategory(@PathVariable id: Long): ResponseEntity<CategoryDTO> {
//        return ResponseEntity.ok()
//    }
}