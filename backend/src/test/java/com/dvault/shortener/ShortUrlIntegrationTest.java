package com.dvault.shortener;

import com.dvault.auth.*;
import com.dvault.security.AccountPrincipal;
import com.dvault.user.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ShortUrlIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ShortUrlRepository urls;
    @Autowired UserAccountRepository users;
    @Autowired RegistrationService registration;
    AccountPrincipal owner;
    AccountPrincipal other;

    @BeforeEach void accounts() {
        urls.deleteAll(); users.deleteAll();
        registration.register(new AuthRequests.Credentials("owner@example.com", "test-only-password"));
        registration.register(new AuthRequests.Credentials("other@example.com", "test-only-password"));
        owner = new AccountPrincipal(users.findByEmail("owner@example.com").orElseThrow());
        other = new AccountPrincipal(users.findByEmail("other@example.com").orElseThrow());
    }
    private Map<String, Object> input(String target, String alias) {
        return new HashMap<>(Map.of("targetUrl", target, "alias", alias, "active", true));
    }
    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, AccountPrincipal principal, Object body) throws Exception {
        return request.with(user(principal)).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(body));
    }
    private long create(String alias) throws Exception {
        var result = mvc.perform(json(post("/api/urls"), owner, input("https://example.com/start", alias)))
            .andExpect(status().isCreated()).andExpect(header().exists("Location")).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test void ownerCanCreateListReadUpdateAndDelete() throws Exception {
        long id = create("Custom_Alias");
        mvc.perform(get("/api/urls/" + id).with(user(owner)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.alias").value("custom_alias"));
        mvc.perform(get("/api/urls").with(user(owner))).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(id));
        var update = input("https://example.org/updated?q=1#part", "changed");
        update.put("active", false); update.put("expiresAt", Instant.now().plusSeconds(3600).toString());
        mvc.perform(json(put("/api/urls/" + id), owner, update)).andExpect(status().isOk())
            .andExpect(jsonPath("$.targetUrl").value(update.get("targetUrl")))
            .andExpect(jsonPath("$.alias").value("changed")).andExpect(jsonPath("$.active").value(false));
        mvc.perform(delete("/api/urls/" + id).with(user(owner)).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/urls/" + id).with(user(owner))).andExpect(status().isNotFound());
        assertEquals(0, urls.count());
    }

    @Test void otherAccountCannotReadUpdateDeleteOrListOwnersUrls() throws Exception {
        long id = create("private-link");
        mvc.perform(get("/api/urls/" + id).with(user(other))).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("URL_NOT_FOUND"));
        mvc.perform(json(put("/api/urls/" + id), other, input("https://attacker.example", "stolen-link"))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/urls/" + id).with(user(other)).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(get("/api/urls").with(user(other))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/urls/" + id).with(user(owner))).andExpect(status().isOk()).andExpect(jsonPath("$.alias").value("private-link"));
        var forged = input("https://example.com", "forged-owner"); forged.put("ownerId", owner.getAccountId());
        mvc.perform(json(post("/api/urls"), other, forged)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "data:text/html,test", "ftp://example.com/file", "https://", "https://user:pass@example.com", "https://example.com:99999", "https://example.com/\r\nInjected", "https://example.com/has space", "https://example.com/\\path"})
    void rejectsUnsafeOrMalformedTargets(String target) throws Exception {
        // Convert escaped control input into a literal CR/LF so JSON transports it safely.
        target = target.replace("\\r\\n", "\r\n");
        mvc.perform(json(post("/api/urls"), owner, input(target, "valid-alias")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_URL"));
        assertEquals(0, urls.count());
    }

    @Test void aliasesAreGlobalUniqueAndGeneratedWhenBlank() throws Exception {
        long first = create("Reserved");
        mvc.perform(json(post("/api/urls"), other, input("https://example.org", "RESERVED")))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALIAS_CONFLICT"));
        long second = create("");
        mvc.perform(get("/api/urls/" + second).with(user(owner)))
            .andExpect(jsonPath("$.alias").value(org.hamcrest.Matchers.matchesPattern("[a-f0-9]{12}")));
        mvc.perform(json(put("/api/urls/" + second), owner, input("https://example.org", "reserved")))
            .andExpect(status().isConflict());
        mvc.perform(json(post("/api/urls"), owner, input("https://example.org", "bad alias")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ALIAS"));
        assertEquals(2, urls.count());
        assertNotEquals(first, second);
    }

    @Test void paginationIsBoundedAndDeterministic() throws Exception {
        create("first-link"); long last = create("second-link");
        mvc.perform(get("/api/urls?page=0&size=1").with(user(owner))).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.content[0].id").value(last));
        mvc.perform(get("/api/urls?page=1&size=1").with(user(owner))).andExpect(status().isOk()).andExpect(jsonPath("$.content.length()").value(1));
        mvc.perform(get("/api/urls?page=-1").with(user(owner))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/urls?size=101").with(user(owner))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/urls?page=invalid").with(user(owner))).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }

    @Test void rejectsPastExpiryAndRequiresAuthenticationAndCsrf() throws Exception {
        var invalid = input("https://example.com", "expired"); invalid.put("expiresAt", Instant.now().minusSeconds(60).toString());
        mvc.perform(json(post("/api/urls"), owner, invalid)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/urls")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/urls").with(user(owner)).contentType("application/json")
            .content(mapper.writeValueAsString(input("https://example.com", "no-csrf")))).andExpect(status().isForbidden());
        assertEquals(0, urls.count());
    }
}
