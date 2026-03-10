package uz.pdp.online_university.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import uz.pdp.online_university.entity.User;
import uz.pdp.online_university.enums.AccessState;
import uz.pdp.online_university.enums.UserStatus;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final UserStatus status;
    private final AccessState accessState;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.status = user.getStatus();
        this.accessState = user.getAccessState();
        this.authorities = buildAuthorities(user);
    }

    private static Collection<? extends GrantedAuthority> buildAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        user.getRoles().forEach(role -> {
            // ROLE_ADMIN, ROLE_TEACHER, etc. — for @PreAuthorize("hasRole('ADMIN')")
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getName().name()));

            // permission keys — for @PreAuthorize("hasAuthority('course.create')")
            role.getPermissions().forEach(permission ->
                    authorities.add(new SimpleGrantedAuthority(permission.getKey()))
            );
        });

        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return status != UserStatus.DEACTIVATED;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accessState == AccessState.ACTIVE
                || accessState == AccessState.TEMPORARY_OVERRIDE;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE;
    }
}