package beer.thierry.budgetplannerrest.controller

import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.budgetplannerrest.service.IUserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/users")
@RestController
class UserController(private val userService: IUserService) {

    @GetMapping("")
    fun getAllUsers(): ResponseEntity<List<UserDTO>> {
        return ResponseEntity.ok(userService.fetchAllUsers())
    }

    @GetMapping("/myself")
    fun getUserByUsername(@AuthenticationPrincipal user: UserDTO): ResponseEntity<UserDTO> {
        return ResponseEntity.ok(userService.fetchUserByUsername(user.username))
    }
}