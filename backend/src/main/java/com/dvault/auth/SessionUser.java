package com.dvault.auth;

import com.dvault.security.AccountPrincipal;
import com.dvault.user.UserAccount;
import java.util.Set;
import java.util.stream.Collectors;

public record SessionUser(Long id, String email, Set<String> roles) {
    static SessionUser from(UserAccount account) {
        return new SessionUser(account.getId(), account.getEmail(), account.getRoles().stream()
            .map(role -> role.getName().name()).collect(Collectors.toUnmodifiableSet()));
    }
    public static SessionUser from(AccountPrincipal principal) {
        return new SessionUser(principal.getAccountId(), principal.getUsername(), principal.getAuthorities().stream()
            .map(authority -> authority.getAuthority().substring("ROLE_".length()))
            .collect(Collectors.toUnmodifiableSet()));
    }
}
