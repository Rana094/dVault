package com.dvault.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    boolean existsByEmail(String email);
    boolean existsByIdAndStatus(Long id, UserAccount.Status status);
    @EntityGraph(attributePaths = "roles")
    Optional<UserAccount> findByEmail(String email);
}
