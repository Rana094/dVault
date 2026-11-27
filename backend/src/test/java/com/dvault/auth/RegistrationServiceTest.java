package com.dvault.auth;

import com.dvault.user.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {
    @Mock UserAccountRepository users;
    @Mock RoleRepository roles;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private RegistrationService service;

    @BeforeEach void setup() { service = new RegistrationService(users, roles, encoder); }

    @Test void storesNormalizedEmailBcryptHashAndOnlyUserRole() {
        when(roles.findByName(Role.Name.USER)).thenReturn(Optional.of(new Role(Role.Name.USER)));
        when(users.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        var result = service.register(new AuthRequests.Credentials("  Alice@Example.COM ", "a-long-test-password"));
        var saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(users).saveAndFlush(saved.capture());
        assertEquals("alice@example.com", result.email());
        assertEquals(java.util.Set.of("USER"), result.roles());
        assertTrue(encoder.matches("a-long-test-password", saved.getValue().getPasswordHash()));
        assertNotEquals("a-long-test-password", saved.getValue().getPasswordHash());
        assertEquals(UserAccount.Status.ACTIVE, saved.getValue().getStatus());
        assertNotNull(saved.getValue().getCreatedAt());
    }

    @Test void refusesExistingEmailWithoutWritingOrChangingItsPassword() {
        when(users.existsByEmail("alice@example.com")).thenReturn(true);
        assertThrows(DuplicateAccountException.class, () -> service.register(
            new AuthRequests.Credentials("Alice@Example.com", "another-test-password")));
        verify(users, never()).saveAndFlush(any());
        verifyNoInteractions(roles);
    }

    @Test void mapsConcurrentUniqueConstraintFailureToConflict() {
        when(roles.findByName(Role.Name.USER)).thenReturn(Optional.of(new Role(Role.Name.USER)));
        when(users.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));
        assertThrows(DuplicateAccountException.class, () -> service.register(
            new AuthRequests.Credentials("alice@example.com", "a-long-test-password")));
    }
}
