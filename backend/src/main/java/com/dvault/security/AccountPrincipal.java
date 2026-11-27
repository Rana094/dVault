package com.dvault.security;

import com.dvault.user.UserAccount;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public final class AccountPrincipal extends User {
    private final Long accountId;

    public AccountPrincipal(UserAccount account) {
        super(account.getEmail(), account.getPasswordHash(),
            account.getStatus() == UserAccount.Status.ACTIVE, true, true, true,
            account.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name())).toList());
        this.accountId = account.getId();
    }
    public Long getAccountId() { return accountId; }
}
