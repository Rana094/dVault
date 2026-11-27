package com.dvault;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FoundationIntegrationTest {
    @Autowired MockMvc mvc;

    @Test void exposesMinimalHealthWithoutAuthentication() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk())
            .andExpect(content().json("{\"status\":\"UP\"}", true));
    }

    @Test void servesBuiltWorkspaceWithoutAuthentication() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/index.html")).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("<title>dVault</title>")));
    }

    @Test void issuesSessionBoundCsrfToken() throws Exception {
        mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk())
            .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
            .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test void rejectsAnonymousPrivateApiAccess() throws Exception {
        mvc.perform(get("/api/files")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
            .andExpect(jsonPath("$.path").value("/api/files"))
            .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test void rejectsMutationWithoutCsrfToken() throws Exception {
        mvc.perform(post("/api/files")).andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
