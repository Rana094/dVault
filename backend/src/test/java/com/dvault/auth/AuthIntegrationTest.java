package com.dvault.auth;

import com.dvault.user.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthIntegrationTest.SecurityProbe.class)
class AuthIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserAccountRepository users;
    @Autowired RegistrationService registration;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    private static final String EMAIL = "alice@example.com";
    private static final String PASSWORD = "a-long-test-password";

    @RestController
    static class SecurityProbe {
        @GetMapping("/api/admin/security-probe") Map<String, String> admin() { return Map.of("access", "admin"); }
    }

    record Browser(MockHttpSession session, String header, String token) {}

    @BeforeEach void clearUsers() { users.deleteAll(); }

    private Browser browser(MockHttpSession session) throws Exception {
        var result = mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
        var body = mapper.readTree(result.getResponse().getContentAsString());
        return new Browser((MockHttpSession) result.getRequest().getSession(), body.get("headerName").asText(), body.get("token").asText());
    }
    private Browser browser() throws Exception { return browser(new MockHttpSession()); }
    private MockHttpServletRequestBuilder mutation(String path, Browser browser, Object body) throws Exception {
        return post(path).session(browser.session()).header(browser.header(), browser.token())
            .contentType("application/json").content(mapper.writeValueAsString(body));
    }
    private Map<String, String> credentials(String email, String password) { return Map.of("email", email, "password", password); }
    private Browser login() throws Exception {
        registration.register(new AuthRequests.Credentials(EMAIL, PASSWORD));
        Browser browser = browser();
        mvc.perform(mutation("/api/auth/login", browser, credentials(EMAIL, PASSWORD))).andExpect(status().isOk());
        return browser(browser.session());
    }

    @Test void registersNormalizedAccountWithoutAutoLoginOrExposingHash() throws Exception {
        var browser = browser();
        mvc.perform(mutation("/api/auth/register", browser, credentials(" Alice@Example.COM ", PASSWORD)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.email").value(EMAIL))
            .andExpect(jsonPath("$.roles[0]").value("USER"))
            .andExpect(jsonPath("$.password").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
        var saved = users.findByEmail(EMAIL).orElseThrow();
        assertTrue(encoder.matches(PASSWORD, saved.getPasswordHash()));
        assertEquals(1, saved.getRoles().size());
        mvc.perform(get("/api/auth/session").session(browser.session())).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest @CsvSource({"not-an-email,a-long-test-password", "alice@example.com,short"})
    void rejectsInvalidRegistration(String email, String password) throws Exception {
        mvc.perform(mutation("/api/auth/register", browser(), credentials(email, password)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        assertEquals(0, users.count());
    }

    @Test void rejectsPasswordsBeyondBcryptByteLimit() throws Exception {
        mvc.perform(mutation("/api/auth/register", browser(), credentials(EMAIL, "é".repeat(40))))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors.password").exists());
        assertEquals(0, users.count());
    }

    @Test void rejectsClientChosenRoleAndMalformedJson() throws Exception {
        var browser = browser();
        mvc.perform(mutation("/api/auth/register", browser, Map.of("email", EMAIL, "password", PASSWORD, "roles", new String[]{"ADMIN"})))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(post("/api/auth/register").session(browser.session()).header(browser.header(), browser.token())
            .contentType("application/json").content("{bad json"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        assertEquals(0, users.count());
    }

    @Test void duplicateEmailIsCaseInsensitiveConflict() throws Exception {
        registration.register(new AuthRequests.Credentials(EMAIL, PASSWORD));
        mvc.perform(mutation("/api/auth/register", browser(), credentials("ALICE@EXAMPLE.COM", "other-valid-password")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EMAIL_CONFLICT"));
        assertEquals(1, users.count());
        assertTrue(encoder.matches(PASSWORD, users.findByEmail(EMAIL).orElseThrow().getPasswordHash()));
    }

    @Test void loginRotatesSessionPersistsAuthenticationAndInvalidatesOldCsrf() throws Exception {
        registration.register(new AuthRequests.Credentials(EMAIL, PASSWORD));
        var browser = browser();
        String previousId = browser.session().getId();
        mvc.perform(mutation("/api/auth/login", browser, credentials("ALICE@EXAMPLE.COM", PASSWORD)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.roles[0]").value("USER"));
        assertNotEquals(previousId, browser.session().getId());
        mvc.perform(get("/api/auth/session").session(browser.session()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(EMAIL));
        mvc.perform(mutation("/api/auth/logout", browser, Map.of())).andExpect(status().isForbidden());
        var refreshed = browser(browser.session());
        mvc.perform(mutation("/api/auth/logout", refreshed, Map.of())).andExpect(status().isNoContent());
        assertTrue(refreshed.session().isInvalid());
        mvc.perform(get("/api/auth/session")).andExpect(status().isUnauthorized());
        var freshAnonymous = browser();
        mvc.perform(mutation("/api/auth/register", freshAnonymous, credentials("bob@example.com", PASSWORD)))
            .andExpect(status().isCreated());
    }

    @Test void refusesWrongAndUnknownCredentialsWithoutCreatingAuthentication() throws Exception {
        registration.register(new AuthRequests.Credentials(EMAIL, PASSWORD));
        var browser = browser();
        for (String email : new String[]{EMAIL, "missing@example.com"}) {
            mvc.perform(mutation("/api/auth/login", browser, credentials(email, "wrong-valid-password")))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }
        mvc.perform(get("/api/auth/session").session(browser.session())).andExpect(status().isUnauthorized());
    }

    @Test void requiresCsrfForAllAuthenticationMutations() throws Exception {
        for (String operation : new String[]{"register", "login", "logout"}) {
            mvc.perform(post("/api/auth/" + operation).contentType("application/json")
                .content(mapper.writeValueAsString(credentials(EMAIL, PASSWORD))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
        assertEquals(0, users.count());
    }

    @Test void enforcesAdminRoleAndNeverInfersItFromLoginPayload() throws Exception {
        var browser = login();
        mvc.perform(get("/api/admin/security-probe").session(browser.session())).andExpect(status().isForbidden());
        mvc.perform(mutation("/api/auth/login", browser, Map.of("email", EMAIL, "password", PASSWORD, "role", "ADMIN")))
            .andExpect(status().isBadRequest());
        long userId = users.findByEmail(EMAIL).orElseThrow().getId();
        jdbc.update("INSERT INTO user_roles (user_id, role_id) SELECT ?, id FROM roles WHERE name = 'ADMIN'", userId);
        var newBrowser = browser();
        mvc.perform(mutation("/api/auth/login", newBrowser, credentials(EMAIL, PASSWORD))).andExpect(status().isOk());
        mvc.perform(get("/api/admin/security-probe").session(newBrowser.session()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.access").value("admin"));
    }

    @Test void suspendedAccountsLoseExistingSessionsAndCannotLogin() throws Exception {
        var browser = login();
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE email = ?", EMAIL);
        mvc.perform(get("/api/auth/session").session(browser.session())).andExpect(status().isUnauthorized());
        assertTrue(browser.session().isInvalid());
        mvc.perform(mutation("/api/auth/login", browser(), credentials(EMAIL, PASSWORD)))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
