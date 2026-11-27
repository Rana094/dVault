package com.dvault.shortener;

import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShortUrlRepository extends JpaRepository<ShortUrl, Long> {
    boolean existsByAlias(String alias);
    Optional<ShortUrl> findByIdAndOwnerId(Long id, Long ownerId);
    Page<ShortUrl> findByOwnerId(Long ownerId, Pageable pageable);
}
