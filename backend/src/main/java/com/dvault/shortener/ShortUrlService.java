package com.dvault.shortener;

import com.dvault.user.UserAccountRepository;
import java.net.URI;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortUrlService {
    private final ShortUrlRepository urls;
    private final UserAccountRepository users;
    private final AliasGenerator generator;

    public ShortUrlService(ShortUrlRepository urls, UserAccountRepository users, AliasGenerator generator) {
        this.urls = urls;
        this.users = users;
        this.generator = generator;
    }

    @Transactional
    public ShortUrlView create(Long ownerId, ShortUrlInput input) {
        String target = validateTarget(input.targetUrl());
        String alias = resolveAlias(input.alias());
        return save(new ShortUrl(users.getReferenceById(ownerId), alias, target, input.expiresAt(), input.active()));
    }

    @Transactional(readOnly = true)
    public Page<ShortUrlView> list(Long ownerId, int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new ShortUrlException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "Page must be nonnegative and size between 1 and 100");
        return urls.findByOwnerId(ownerId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")))
            .map(ShortUrlView::from);
    }

    @Transactional(readOnly = true)
    public ShortUrlView get(Long ownerId, Long id) { return ShortUrlView.from(owned(ownerId, id)); }

    @Transactional
    public ShortUrlView update(Long ownerId, Long id, ShortUrlInput input) {
        var url = owned(ownerId, id);
        String target = validateTarget(input.targetUrl());
        String alias = input.alias() == null || input.alias().isBlank() ? url.getAlias() : validateAlias(input.alias());
        if (!alias.equals(url.getAlias()) && urls.existsByAlias(alias)) throw collision();
        url.update(alias, target, input.expiresAt(), input.active());
        return save(url);
    }

    @Transactional
    public void delete(Long ownerId, Long id) { urls.delete(owned(ownerId, id)); }

    private ShortUrl owned(Long ownerId, Long id) {
        return urls.findByIdAndOwnerId(id, ownerId).orElseThrow(() ->
            new ShortUrlException(HttpStatus.NOT_FOUND, "URL_NOT_FOUND", "Short link not found"));
    }

    private ShortUrlView save(ShortUrl url) {
        try { return ShortUrlView.from(urls.saveAndFlush(url)); }
        catch (DataIntegrityViolationException conflict) { throw collision(); }
    }

    private String resolveAlias(String requested) {
        if (requested != null && !requested.isBlank()) {
            String alias = validateAlias(requested);
            if (urls.existsByAlias(alias)) throw collision();
            return alias;
        }
        for (int attempt = 0; attempt < 8; attempt++) {
            String candidate = generator.nextAlias();
            if (!urls.existsByAlias(candidate)) return candidate;
        }
        throw collision();
    }

    private String validateAlias(String alias) {
        if (!alias.matches("[a-z0-9][a-z0-9_-]{2,31}"))
            throw new ShortUrlException(HttpStatus.BAD_REQUEST, "INVALID_ALIAS", "Alias must be 3–32 lowercase letters, digits, hyphens or underscores, starting with a letter or digit");
        return alias;
    }

    private String validateTarget(String raw) {
        if (raw == null || raw.chars().anyMatch(Character::isISOControl) || raw.contains("\\")) throw invalidTarget();
        String target = raw.strip();
        try {
            URI uri = URI.create(target);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null || uri.getPort() > 65535)
                throw invalidTarget();
        } catch (IllegalArgumentException malformed) { throw invalidTarget(); }
        return target;
    }

    private ShortUrlException invalidTarget() {
        return new ShortUrlException(HttpStatus.BAD_REQUEST, "INVALID_URL", "Use a valid HTTP(S) URL with a host, without credentials or control characters");
    }
    private ShortUrlException collision() {
        return new ShortUrlException(HttpStatus.CONFLICT, "ALIAS_CONFLICT", "That alias is already in use; choose another or generate one");
    }
}
