package com.dvault.auth;

import com.dvault.user.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final UserAccountRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public RegistrationService(UserAccountRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Transactional
    public SessionUser register(AuthRequests.Credentials credentials) {
        String email = AuthRequests.normalizeEmail(credentials.email());
        if (users.existsByEmail(email)) throw new DuplicateAccountException();
        Role role = roles.findByName(Role.Name.USER)
            .orElseThrow(() -> new IllegalStateException("Required USER role is not configured"));
        UserAccount account = new UserAccount(email, encoder.encode(credentials.password()), role);
        try {
            return SessionUser.from(users.saveAndFlush(account));
        } catch (DataIntegrityViolationException conflict) {
            // The database unique constraint also protects concurrent registrations.
            throw new DuplicateAccountException();
        }
    }
}
