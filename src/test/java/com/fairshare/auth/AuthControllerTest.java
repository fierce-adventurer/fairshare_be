package com.fairshare.auth;

import com.fairshare.auth.dto.AuthResponse;
import com.fairshare.auth.dto.LoginRequest;
import com.fairshare.auth.dto.RegisterRequest;
import com.fairshare.auth.service.AuthService;
import com.fairshare.shared.security.JwtTokenProvider;
import com.fairshare.user.dto.UserProfileResponse;
import com.fairshare.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void testRegisterSuccess() throws Exception {
        UUID userId = UUID.randomUUID();
        User dummyUser = new User(userId, "test@example.com", "hash", "Test User");
        UserProfileResponse profile = UserProfileResponse.from(dummyUser);
        AuthResponse mockResponse = new AuthResponse("access.token.jwt", "refresh-uuid", profile);

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        String json = """
                {
                    "name": "Test User",
                    "email": "test@example.com",
                    "password": "Password123"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access.token.jwt"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
    }

    @Test
    void testLoginInvalidBodyValidation() throws Exception {
        String invalidJson = """
                {
                    "email": "not-an-email",
                    "password": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGoogleCallbackMobileRedirect() throws Exception {
        UserProfileResponse profile = UserProfileResponse.from(
                new User(UUID.randomUUID(), "mobile@example.com", "hash", "Mobile User")
        );
        AuthResponse mockResponse = new AuthResponse("mobile.jwt.token", "mobile-refresh", profile);
        when(authService.handleGoogleCallback("valid-code")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/auth/oauth/google/callback")
                        .param("code", "valid-code")
                        .param("state", "mobile:" + UUID.randomUUID()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("com.fairshare.app://auth/callback?access_token=mobile.jwt.token&refresh_token=mobile-refresh")));
    }

    @Test
    void testGoogleCallbackWebRedirect() throws Exception {
        UserProfileResponse profile = UserProfileResponse.from(
                new User(UUID.randomUUID(), "web@example.com", "hash", "Web User")
        );
        AuthResponse mockResponse = new AuthResponse("web.jwt.token", "web-refresh", profile);
        when(authService.handleGoogleCallback("valid-code")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/auth/oauth/google/callback")
                        .param("code", "valid-code")
                        .param("state", "web:" + UUID.randomUUID()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("http://localhost:5173/auth/callback?access_token=web.jwt.token&refresh_token=web-refresh")));
    }

    @Test
    void testGitHubCallbackMobileRedirect() throws Exception {
        UserProfileResponse profile = UserProfileResponse.from(
                new User(UUID.randomUUID(), "gh-mobile@example.com", "hash", "GH Mobile User")
        );
        AuthResponse mockResponse = new AuthResponse("gh.jwt.token", "gh-refresh", profile);
        when(authService.handleGithubCallback("gh-code")).thenReturn(mockResponse);

        mockMvc.perform(get("/api/v1/auth/oauth/github/callback")
                        .param("code", "gh-code")
                        .param("state", "mobile:" + UUID.randomUUID()))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("com.fairshare.app://auth/callback?access_token=gh.jwt.token&refresh_token=gh-refresh")));
    }
}
