package com.dvault.auth;

import com.dvault.security.AccountPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final RegistrationService registration;
    private final SessionAuthenticationService sessions;

    public AuthController(RegistrationService registration, SessionAuthenticationService sessions) {
        this.registration = registration;
        this.sessions = sessions;
    }

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public SessionUser register(@Valid @RequestBody AuthRequests.Credentials credentials) {
        return registration.register(credentials);
    }

    @PostMapping("/login")
    public SessionUser login(@Valid @RequestBody AuthRequests.Credentials credentials,
                            HttpServletRequest request, HttpServletResponse response) {
        return sessions.login(credentials, request, response);
    }

    @GetMapping("/session")
    public SessionUser session(@AuthenticationPrincipal AccountPrincipal principal) {
        return SessionUser.from(principal);
    }
}
