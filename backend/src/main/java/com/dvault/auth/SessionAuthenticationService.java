package com.dvault.auth;

import com.dvault.security.AccountPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
public class SessionAuthenticationService {
    private final AuthenticationManager manager;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final SecurityContextRepository contexts;

    public SessionAuthenticationService(AuthenticationManager manager,
            SessionAuthenticationStrategy sessionStrategy, SecurityContextRepository contexts) {
        this.manager = manager;
        this.sessionStrategy = sessionStrategy;
        this.contexts = contexts;
    }

    public SessionUser login(AuthRequests.Credentials credentials,
                             HttpServletRequest request, HttpServletResponse response) {
        var authentication = manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
            credentials.email(), credentials.password()));
        sessionStrategy.onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return SessionUser.from((AccountPrincipal) authentication.getPrincipal());
    }
}
