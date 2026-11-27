package com.dvault.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class AuthRequests {
    private AuthRequests() {}

    public static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    public record Credentials(@NotBlank @Email @Size(max = 254) String email,
                              @NotBlank @Size(min = 12, max = 72) String password) {
        public Credentials { email = normalizeEmail(email); }

        @JsonIgnore
        @AssertTrue(message = "Password must not exceed 72 UTF-8 bytes")
        public boolean isPasswordWithinLimit() {
            return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
        }

        @Override public String toString() { return "Credentials[redacted]"; }
    }
}
