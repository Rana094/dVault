package com.dvault.security;

import com.dvault.common.ApiError;
import com.dvault.user.UserAccount;
import com.dvault.user.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

// Instantiated only inside the security chain to avoid double servlet registration.
final class AccountStatusFilter extends OncePerRequestFilter {
    private final UserAccountRepository users;
    private final ObjectMapper mapper;

    AccountStatusFilter(UserAccountRepository users, ObjectMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal
                && !users.existsByIdAndStatus(principal.getAccountId(), UserAccount.Status.ACTIVE)) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), ApiError.of(401, "UNAUTHENTICATED", "Authentication required", request.getRequestURI()));
            return;
        }
        chain.doFilter(request, response);
    }
}
