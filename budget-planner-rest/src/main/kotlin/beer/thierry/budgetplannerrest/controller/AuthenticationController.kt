package beer.thierry.budgetplannerrest.controller

import beer.thierry.budgetplannerrest.model.auth.AuthRegisterRequest
import beer.thierry.budgetplannerrest.model.auth.AuthRequest
import beer.thierry.budgetplannerrest.model.auth.AuthResponse
import beer.thierry.budgetplannerrest.model.user.UserDTO
import beer.thierry.budgetplannerrest.service.auth.IAuthService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api/auth")
@RestController
class AuthenticationController(private val authService: IAuthService) {
    @PostMapping("/login")
    fun login(@RequestBody request: AuthRequest): ResponseEntity<AuthResponse> {
        val response = authService.authenticate(request)
        return ResponseEntity.ok(response)
    }

    @PostMapping("/register")
    fun register(@RequestBody form: AuthRegisterRequest): ResponseEntity<AuthResponse> {
        val response = authService.register(form)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/confirm")
    fun confirm(@RequestParam token: String, @RequestParam username: String): ResponseEntity<String> {
        val confirmed = authService.confirmRegistration(token, username)
        return if (confirmed) {
            ResponseEntity.ok("Account confirmed successfully")
        } else {
            ResponseEntity.badRequest().body("Invalid confirmation token")
        }
    }

    @GetMapping("/verify")
    fun verify(@AuthenticationPrincipal authenticatedUser: UserDTO): ResponseEntity<String> {
        return ResponseEntity.ok("ok")
    }
}
