package com.example.schemaregistry.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureWebMvc
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testUnauthenticatedAccess() throws Exception {
        mockMvc.perform(get("/subjects/test/versions/1"))
               .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void testAuthenticatedAccess() throws Exception {
        mockMvc.perform(get("/subjects/test/versions/1"))
               .andExpect(status().isNotFound()); // Assuming no schema exists
    }
}