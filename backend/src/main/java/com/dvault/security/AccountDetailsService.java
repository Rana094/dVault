package com.dvault.security;

import com.dvault.auth.AuthRequests;
import com.dvault.user.UserAccountRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDetailsService implements UserDetailsService {
    private final UserAccountRepository users;

    public AccountDetailsService(UserAccountRepository users) { this.users = users; }

    @Override @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmail(AuthRequests.normalizeEmail(email)).map(AccountPrincipal::new)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }
}
