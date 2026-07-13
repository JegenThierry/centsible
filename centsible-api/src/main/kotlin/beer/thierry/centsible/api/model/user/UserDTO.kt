package beer.thierry.centsible.api.model.user

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

/** Authenticated principal: the [UserDetails] carried through Spring Security; password is intentionally absent. */
data class UserDTO(
    var id: UUID = UUID.randomUUID(),
    @get:JvmName("getUsernameValue")
    var username: String = "",
    var email: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var name: String = "",
    var profilePicture: String? = null,
    var locale: String = "en",
    var defaultCurrency: String = "EUR",
    /** True when this user is the instance admin designated by configuration (admin.enabled + admin.username). */
    var admin: Boolean = false,
    var authoritiesList: Collection<GrantedAuthority> =
        if (admin) listOf(SimpleGrantedAuthority("ROLE_USER"), SimpleGrantedAuthority("ROLE_ADMIN"))
        else listOf(SimpleGrantedAuthority("ROLE_USER"))

) : UserDetails {
    override fun getAuthorities() = authoritiesList
    override fun getPassword() = null
    override fun getUsername() = username
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true
}
