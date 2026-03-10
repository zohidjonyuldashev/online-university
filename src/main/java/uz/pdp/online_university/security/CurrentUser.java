package uz.pdp.online_university.security;

import org.springframework.security.core.GrantedAuthority;
import uz.pdp.online_university.enums.RoleName;

import java.util.Set;
import java.util.stream.Collectors;

public class CurrentUser {

    private final CustomUserDetails userDetails;
    private final Set<RoleName> roles;

    public CurrentUser(CustomUserDetails userDetails) {
        this.userDetails = userDetails;
        this.roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5))
                .map(RoleName::valueOf)
                .collect(Collectors.toSet());
    }

    public Long getId() {
        return userDetails.getId();
    }

    public String getEmail() {
        return userDetails.getEmail();
    }

    public boolean hasRole(RoleName role) {
        return roles.contains(role);
    }

    public boolean hasAnyRole(Set<RoleName> checkRoles) {
        return checkRoles.stream().anyMatch(roles::contains);
    }

    public Set<RoleName> getRoles() {
        return roles;
    }

    public String getRoleNames() {
        return roles.stream()
                .map(RoleName::name)
                .collect(Collectors.joining(", "));
    }
}
