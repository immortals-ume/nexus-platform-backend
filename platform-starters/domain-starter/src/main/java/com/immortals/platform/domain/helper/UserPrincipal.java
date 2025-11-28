package com.immortals.platform.domain.helper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.immortals.platform.domain.entity.Roles;
import com.immortals.platform.domain.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@Getter
public class UserPrincipal implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long userId;
    private final String userName;

    @JsonIgnore
    private final String password;

    private final String email;

    private final boolean accountNonExpired;
    private final boolean accountNonLocked;
    private final boolean credentialsNonExpired;
    private final boolean enabled;

    private final transient Collection<? extends GrantedAuthority> authorities;

    private final transient Collection<String> permissions;

    private UserPrincipal(Long userId,
                          String userName,
                          String password,
                          String email,
                          Collection<? extends GrantedAuthority> authorities,
                          Collection<String> permissions,
                          boolean accountNonExpired,
                          boolean accountNonLocked,
                          boolean credentialsNonExpired,
                          boolean enabled) {
        this.userId = userId;
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.authorities = authorities != null ? authorities : Collections.emptyList();
        this.permissions = permissions != null ? permissions : Collections.emptyList();
        this.accountNonExpired = accountNonExpired;
        this.accountNonLocked = accountNonLocked;
        this.credentialsNonExpired = credentialsNonExpired;
        this.enabled = enabled;
    }

    /**
     * Factory method to build the principal from User entity
     */
    public static UserPrincipal create(User user, Collection<Roles> roles, Collection<String> permissions) {
        Collection<? extends GrantedAuthority> authorities = extractAuthorities(roles);
        return new UserPrincipal(
                user.getId() != null ? user.getId().getMostSignificantBits() : null,
                user.getUserName(),
                user.getPassword(),
                user.getEmail(),
                authorities,
                permissions,
                Boolean.TRUE.equals(user.getAccountNonExpired()),
                Boolean.TRUE.equals(user.getAccountNonLocked()) && !Boolean.TRUE.equals(user.getAccountLocked()),
                Boolean.TRUE.equals(user.getCredentialsNonExpired()),
                Boolean.TRUE.equals(user.getActiveInd())
        );
    }

    private static Collection<? extends GrantedAuthority> extractAuthorities(Collection<Roles> roles) {
        if (roles == null) return Collections.emptyList();
        return roles.stream()
                .filter(Objects::nonNull)
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .toList();
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof UserPrincipal that)) return false;
        return Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId);
    }
}
