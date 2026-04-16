package beer.thierry.budgetplanner.api.model.user

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

data class UserDTO(
    var id: UUID = UUID.randomUUID(),
    @get:JvmName("getUsernameValue")
    var username: String = "",
    var email: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var name: String = "",
    var profilePicture: String? = null,
    var authoritiesList: Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority("ROLE_USER"))

) : UserDetails {
    override fun getAuthorities() = authoritiesList
    override fun getPassword() = null
    override fun getUsername() = username
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true
}
