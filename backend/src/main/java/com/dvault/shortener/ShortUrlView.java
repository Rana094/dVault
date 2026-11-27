package com.dvault.shortener;

import java.time.Instant;

public record ShortUrlView(Long id, String alias, String targetUrl, Instant expiresAt, boolean active) {
    static ShortUrlView from(ShortUrl url) {
        return new ShortUrlView(url.getId(), url.getAlias(), url.getTargetUrl(), url.getExpiresAt(), url.isActive());
    }
}
