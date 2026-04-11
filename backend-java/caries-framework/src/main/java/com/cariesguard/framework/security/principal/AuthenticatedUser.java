package com.cariesguard.framework.security.principal;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedUser implements UserDetails {

    private final Long userId;
    private final Long orgId;
    private final String username;
    private final String password;
    private final String displayName;
    private final boolean enabled;
    private final List<String> roleCodes;
    private final List<SimpleGrantedAuthority> authorities;

    public AuthenticatedUser(Long userId,
                             Long orgId,
                             String username,
                             String password,
                             String displayName,
                             boolean enabled,
                             List<String> roleCodes) {
        this.userId = userId;
        this.orgId = orgId;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.enabled = enabled;
        this.roleCodes = roleCodes;
        this.authorities = roleCodes.stream()
                .map(roleCode -> new SimpleGrantedAuthority("ROLE_" + roleCode))
                .toList();
    }

    public Long getUserId() {
        return userId;
    }

    public Long getOrgId() {
        return orgId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
