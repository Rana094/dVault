package com.dvault.shortener;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.Locale;

public record ShortUrlInput(@NotBlank @Size(max = 2048) String targetUrl,
                           @Size(max = 32) String alias,
                           @Future Instant expiresAt, Boolean active) {
    public ShortUrlInput {
        if (alias != null) alias = alias.strip().toLowerCase(Locale.ROOT);
        if (active == null) active = true;
    }
}
